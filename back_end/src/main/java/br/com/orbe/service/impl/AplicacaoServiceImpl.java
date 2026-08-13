package br.com.orbe.service.impl;

import br.com.orbe.dao.jdbc.AplicacaoTransacaoDaoJdbc;
import br.com.orbe.dao.jdbc.CarteiraVacinalDaoJdbc;
import br.com.orbe.dto.RegistrarAplicacaoRequest;
import br.com.orbe.dto.CarteiraVacinalItem;
import br.com.orbe.exception.BusinessException;
import br.com.orbe.model.Aplicacao;
import br.com.orbe.service.AplicacaoService;

import java.util.List;

public class AplicacaoServiceImpl implements AplicacaoService {

    private final CarteiraVacinalDaoJdbc carteiraDao;
    private final AplicacaoTransacaoDaoJdbc transacaoDao;

    public AplicacaoServiceImpl(
            AplicacaoTransacaoDaoJdbc transacaoDao,
            CarteiraVacinalDaoJdbc carteiraDao
    ) {
        this.transacaoDao = transacaoDao;
        this.carteiraDao = carteiraDao;
    }

    @Override
    public Aplicacao registrar(RegistrarAplicacaoRequest request) {
        validar(request);
        return transacaoDao.registrar(request);
    }

    @Override
    public List<CarteiraVacinalItem> listarCarteira(Long usuarioId, Long dependenteId) {
        validarPaciente(usuarioId, dependenteId);
        return usuarioId != null
                ? carteiraDao.listarPorUsuario(usuarioId)
                : carteiraDao.listarPorDependente(dependenteId);
    }

    private void validar(RegistrarAplicacaoRequest request) {
        if (request == null) throw new BusinessException("Os dados da aplicação são obrigatórios.");
        validarPaciente(request.usuarioId(), request.dependenteId());
        if (request.funcionarioId() == null) throw new BusinessException("O profissional é obrigatório.");
        if (request.loteId() == null) throw new BusinessException("O lote é obrigatório.");
        if (request.dose() == null || request.dose().isBlank()) throw new BusinessException("A dose é obrigatória.");
        if (request.dataAplicacao() == null) throw new BusinessException("A data da aplicação é obrigatória.");
        if (request.dataAplicacao().isAfter(java.time.LocalDateTime.now())) {
            throw new BusinessException("A data da aplicação não pode estar no futuro.");
        }
        if (request.tipoAtendimento() == null) throw new BusinessException("O tipo de atendimento é obrigatório.");
        if (request.viaAdministracao() == null || request.viaAdministracao().isBlank()) throw new BusinessException("A via de administração é obrigatória.");
        if (request.localAplicacao() == null || request.localAplicacao().isBlank()) throw new BusinessException("O local de aplicação é obrigatório.");
    }

    private void validarPaciente(Long usuarioId, Long dependenteId) {
        if ((usuarioId == null) == (dependenteId == null)) {
            throw new BusinessException("Informe somente o titular ou o dependente.");
        }
    }
}
