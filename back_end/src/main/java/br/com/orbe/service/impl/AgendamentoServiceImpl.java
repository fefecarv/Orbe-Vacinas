package br.com.orbe.service.impl;

import br.com.orbe.dao.AgendamentoDao;
import br.com.orbe.dao.DependenteDao;
import br.com.orbe.dao.UsuarioDao;
import br.com.orbe.dao.VacinaDao;
import br.com.orbe.dao.ConvenioDao;
import br.com.orbe.dao.jdbc.UsuarioConvenioDaoJdbc;
import br.com.orbe.dto.AnaliseConvenio;
import br.com.orbe.dto.CriarAgendamentoRequest;
import br.com.orbe.dto.AgendaDiariaItem;
import br.com.orbe.dao.jdbc.AgendaDiariaDaoJdbc;
import br.com.orbe.exception.BusinessException;
import br.com.orbe.exception.NotFoundException;
import br.com.orbe.model.Agendamento;
import br.com.orbe.model.enums.StatusAgendamento;
import br.com.orbe.model.enums.TipoAtendimento;
import br.com.orbe.service.AgendamentoService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class AgendamentoServiceImpl implements AgendamentoService {

    private final AgendamentoDao agendamentoDao;
    private final UsuarioDao usuarioDao;
    private final DependenteDao dependenteDao;
    private final VacinaDao vacinaDao;
    private final AgendaDiariaDaoJdbc agendaDiariaDao;
    private final ConvenioDao convenioDao;
    private final UsuarioConvenioDaoJdbc usuarioConvenioDao;
    private final br.com.orbe.dao.jdbc.ConfiguracaoAgendaDaoJdbc configuracaoAgendaDao;

    public AgendamentoServiceImpl(
            AgendamentoDao agendamentoDao,
            UsuarioDao usuarioDao,
            DependenteDao dependenteDao,
            VacinaDao vacinaDao,
            AgendaDiariaDaoJdbc agendaDiariaDao,
            ConvenioDao convenioDao,
            UsuarioConvenioDaoJdbc usuarioConvenioDao,
            br.com.orbe.dao.jdbc.ConfiguracaoAgendaDaoJdbc configuracaoAgendaDao
    ) {
        this.agendamentoDao = agendamentoDao;
        this.usuarioDao = usuarioDao;
        this.dependenteDao = dependenteDao;
        this.vacinaDao = vacinaDao;
        this.agendaDiariaDao = agendaDiariaDao;
        this.convenioDao = convenioDao;
        this.usuarioConvenioDao = usuarioConvenioDao;
        this.configuracaoAgendaDao = configuracaoAgendaDao;
    }

    @Override
    public Agendamento criar(CriarAgendamentoRequest request) {
        if (request == null) {
            throw new BusinessException("Os dados do agendamento sao obrigatorios.");
        }
        validarPaciente(request.usuarioId(), request.dependenteId());
        var vacina = vacinaDao.buscarPorId(request.vacinaId())
                .orElseThrow(() -> new NotFoundException("Vacina não encontrada."));
        var nascimento=request.usuarioId()!=null?usuarioDao.buscarPorId(request.usuarioId()).orElseThrow().getDataNascimento():dependenteDao.buscarPorId(request.dependenteId()).orElseThrow().getDataNascimento();
        long idadeMeses=java.time.temporal.ChronoUnit.MONTHS.between(nascimento,LocalDate.now());
        if(idadeMeses<vacina.getIdadeMinimaMeses()||(vacina.getIdadeMaximaMeses()!=null&&idadeMeses>vacina.getIdadeMaximaMeses()))throw new BusinessException("Esta vacina não é indicada para a idade da pessoa selecionada.");
        if (request.dataAgendamento() == null || request.dataAgendamento().isBefore(LocalDateTime.now())) {
            throw new BusinessException("A data do agendamento deve ser futura.");
        }
        if (!horariosDisponiveis(request.dataAgendamento().toLocalDate(), request.unidade()).contains(request.dataAgendamento().toLocalTime())) {
            throw new BusinessException("O horário selecionado não está disponível para esta unidade.");
        }
        if (request.tipoAtendimento() == TipoAtendimento.CONVENIO && request.convenioId() == null) {
            throw new BusinessException("Selecione um convênio para esse atendimento.");
        }
        if (request.tipoAtendimento() == TipoAtendimento.PARTICULAR && request.convenioId() != null) {
            throw new BusinessException("Atendimento particular não pode utilizar convênio.");
        }

        Agendamento agendamento = new Agendamento();
        agendamento.setProtocolo(gerarProtocolo());
        agendamento.setUsuarioId(request.usuarioId());
        agendamento.setDependenteId(request.dependenteId());
        agendamento.setVacinaId(request.vacinaId());
        agendamento.setConvenioId(request.convenioId());
        agendamento.setDataAgendamento(request.dataAgendamento());
        agendamento.setUnidade(request.unidade());
        agendamento.setSala(request.sala());
        agendamento.setDosePrevista(request.dosePrevista());
        agendamento.setTipoAtendimento(request.tipoAtendimento());
        agendamento.setValorEstimado(
                analisarConvenio(request.vacinaId(), request.convenioId()).valorPaciente()
        );
        agendamento.setStatus(StatusAgendamento.PENDENTE);
        return agendamentoDao.salvar(agendamento);
    }

    @Override
    public AnaliseConvenio analisarConvenio(Long vacinaId, Long convenioId) {
        var vacina = vacinaDao.buscarPorId(vacinaId)
                .orElseThrow(() -> new NotFoundException("Vacina não encontrada."));
        BigDecimal base = vacina.getValorBase().setScale(2, RoundingMode.HALF_UP);
        if (convenioId == null) {
            return new AnaliseConvenio(base, "PARTICULAR", BigDecimal.ZERO,
                    BigDecimal.ZERO, base, "APROVADO", "Atendimento particular.");
        }
        var carteirinha = usuarioConvenioDao.buscar(convenioId)
                .orElseThrow(() -> new NotFoundException("Carteirinha não encontrada."));
        var convenio = convenioDao.buscarPorId(carteirinha.getConvenioId())
                .orElseThrow(() -> new NotFoundException("Convênio não encontrado."));
        if (!convenio.isAtivo() || !carteirinha.isAtivo()) {
            throw new BusinessException("A carteirinha está inativa ou vencida.");
        }
        var tipo = convenio.getTipoCobertura();
        BigDecimal paciente;
        BigDecimal percentual = convenio.getPercentualDesconto() == null
                ? BigDecimal.ZERO : convenio.getPercentualDesconto();
        String status = "APROVADO";
        String mensagem = "Cobertura calculada automaticamente.";
        switch (tipo) {
            case INTEGRAL -> paciente = BigDecimal.ZERO;
            case PERCENTUAL -> paciente = base.subtract(
                    base.multiply(percentual).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            case COPARTICIPACAO -> paciente = convenio.getValorCoparticipacao() == null
                    ? base : convenio.getValorCoparticipacao().min(base);
            case SEM_COBERTURA -> { paciente = base; status = "SEM_COBERTURA"; mensagem = "O plano não oferece cobertura para este atendimento."; }
            case ANALISE_MANUAL -> { paciente = null; status = "ANALISE_MANUAL"; mensagem = "Este plano exige análise manual."; }
            default -> throw new BusinessException("Tipo de cobertura inválido.");
        }
        BigDecimal coberto = paciente == null ? null : base.subtract(paciente).max(BigDecimal.ZERO);
        return new AnaliseConvenio(base, tipo.name(), percentual, coberto, paciente, status, mensagem);
    }

    @Override
    public Agendamento buscar(Long id) {
        return obter(id);
    }

    @Override
    public Agendamento cancelar(Long id, String motivo) {
        Agendamento agendamento = normalizarVencido(obter(id));
        if (!podeSerGerenciado(agendamento)) {
            throw new BusinessException("Somente agendamentos futuros pendentes ou confirmados podem ser cancelados.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new BusinessException("Informe o motivo do cancelamento.");
        }
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        agendamento.setMotivoCancelamento(motivo);
        agendamento.setCanceladoEm(LocalDateTime.now());
        return agendamentoDao.atualizar(agendamento);
    }

    @Override
    public Agendamento reagendar(Long id, LocalDateTime novaData) {
        Agendamento agendamento = normalizarVencido(obter(id));
        if (novaData == null || novaData.isBefore(LocalDateTime.now())) {
            throw new BusinessException("A nova data deve ser futura.");
        }
        if (!podeSerGerenciado(agendamento)) {
            throw new BusinessException("Somente agendamentos futuros pendentes ou confirmados podem ser reagendados.");
        }
        agendamento.setDataAgendamento(novaData);
        agendamento.setStatus(StatusAgendamento.CONFIRMADO);
        agendamento.setMotivoCancelamento(null);
        agendamento.setCanceladoEm(null);
        return agendamentoDao.atualizar(agendamento);
    }

    @Override
    public List<Agendamento> listarAgenda(LocalDate data) {
        if (data == null) {
            throw new BusinessException("A data da agenda é obrigatória.");
        }
        return agendamentoDao.listarPorData(data);
    }

    @Override
    public List<Agendamento> listarPaciente(Long usuarioId, Long dependenteId) {
        validarPaciente(usuarioId, dependenteId);
        List<Agendamento> agendamentos = usuarioId != null
                ? agendamentoDao.listarPorUsuario(usuarioId)
                : agendamentoDao.listarPorDependente(dependenteId);
        agendamentos.replaceAll(this::normalizarVencido);
        return agendamentos;
    }

    @Override
    public List<AgendaDiariaItem> listarAgendaDetalhada(LocalDate data, String unidade) {
        if (data == null) throw new BusinessException("A data da agenda é obrigatória.");
        if (unidade == null || unidade.isBlank()) throw new BusinessException("A unidade da agenda é obrigatória.");
        return agendaDiariaDao.listar(data, unidade);
    }

    @Override public List<java.time.LocalTime> horariosDisponiveis(LocalDate data,String unidade){if(data==null||data.isBefore(LocalDate.now()))return List.of();return configuracaoAgendaDao.horarios(data,unidade==null||unidade.isBlank()?"Orbe Centro":unidade).stream().filter(h->data.isAfter(LocalDate.now())||h.isAfter(java.time.LocalTime.now())).toList();}

    @Override
    public Agendamento atualizarStatus(Long id, StatusAgendamento novoStatus) {
        if (novoStatus == null) throw new BusinessException("O novo status é obrigatório.");
        Agendamento agendamento = obter(id);
        StatusAgendamento atual = agendamento.getStatus();
        boolean permitido = (atual == StatusAgendamento.PENDENTE || atual == StatusAgendamento.CONFIRMADO)
                && novoStatus == StatusAgendamento.ESPERA
                || atual == StatusAgendamento.ESPERA && novoStatus == StatusAgendamento.EM_ATENDIMENTO;
        if (!permitido) throw new BusinessException("Transição de status não permitida.");
        agendamento.setStatus(novoStatus);
        return agendamentoDao.atualizar(agendamento);
    }

    private Agendamento obter(Long id) {
        return agendamentoDao.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Agendamento não encontrado."));
    }

    private Agendamento normalizarVencido(Agendamento agendamento) {
        boolean aguardando = agendamento.getStatus() == StatusAgendamento.PENDENTE
                || agendamento.getStatus() == StatusAgendamento.CONFIRMADO;
        if (aguardando && agendamento.getDataAgendamento().isBefore(LocalDateTime.now())) {
            agendamento.setStatus(StatusAgendamento.FALTOU);
            return agendamentoDao.atualizar(agendamento);
        }
        return agendamento;
    }

    private boolean podeSerGerenciado(Agendamento agendamento) {
        return agendamento.getDataAgendamento().isAfter(LocalDateTime.now())
                && (agendamento.getStatus() == StatusAgendamento.PENDENTE
                || agendamento.getStatus() == StatusAgendamento.CONFIRMADO);
    }

    private void validarPaciente(Long usuarioId, Long dependenteId) {
        if ((usuarioId == null) == (dependenteId == null)) {
            throw new BusinessException("Informe somente o titular ou o dependente.");
        }
        if (usuarioId != null && usuarioDao.buscarPorId(usuarioId).isEmpty()) {
            throw new NotFoundException("Usuário não encontrado.");
        }
        if (dependenteId != null && dependenteDao.buscarPorId(dependenteId).isEmpty()) {
            throw new NotFoundException("Dependente não encontrado.");
        }
    }

    private String gerarProtocolo() {
        return "ORB-AGE-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-"
                + ThreadLocalRandom.current().nextInt(100, 1000);
    }
}
