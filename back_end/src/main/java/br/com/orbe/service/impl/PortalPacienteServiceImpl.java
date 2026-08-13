package br.com.orbe.service.impl;

import br.com.orbe.dao.DependenteDao;
import br.com.orbe.dao.ConvenioDao;
import br.com.orbe.dao.jdbc.RecomendacaoDetalheDaoJdbc;
import br.com.orbe.dto.RecomendacaoVacinalItem;
import br.com.orbe.exception.BusinessException;
import br.com.orbe.model.Dependente;
import br.com.orbe.model.Convenio;
import br.com.orbe.model.UsuarioConvenio;
import br.com.orbe.dao.jdbc.UsuarioConvenioDaoJdbc;
import br.com.orbe.service.PortalPacienteService;
import br.com.orbe.model.enums.StatusCadastro;

import java.time.LocalDate;
import java.util.List;

public class PortalPacienteServiceImpl implements PortalPacienteService {
    private final DependenteDao dependenteDao;
    private final RecomendacaoDetalheDaoJdbc recomendacaoDao;
    private final ConvenioDao convenioDao;
    private final UsuarioConvenioDaoJdbc usuarioConvenioDao;

    public PortalPacienteServiceImpl(
            DependenteDao dependenteDao,
            RecomendacaoDetalheDaoJdbc recomendacaoDao,
            ConvenioDao convenioDao,
            UsuarioConvenioDaoJdbc usuarioConvenioDao
    ) {
        this.dependenteDao = dependenteDao;
        this.recomendacaoDao = recomendacaoDao;
        this.convenioDao = convenioDao;
        this.usuarioConvenioDao = usuarioConvenioDao;
    }

    @Override
    public List<Dependente> listarDependentes(Long usuarioId) {
        return dependenteDao.listarPorResponsavel(usuarioId).stream()
                .filter(dependente -> dependente.getStatus() == StatusCadastro.ATIVO)
                .toList();
    }

    @Override
    public List<RecomendacaoVacinalItem> listarRecomendacoes(Long usuarioId, Long dependenteId) {
        if ((usuarioId == null) == (dependenteId == null)) {
            throw new BusinessException("Informe somente o titular ou o dependente.");
        }
        return usuarioId != null
                ? recomendacaoDao.listarPorUsuario(usuarioId)
                : recomendacaoDao.listarPorDependente(dependenteId);
    }

    @Override
    public List<UsuarioConvenio> listarConvenios(Long usuarioId) { return usuarioConvenioDao.listarPorUsuario(usuarioId); }
    public List<Convenio> listarConveniosAceitos(){return convenioDao.listarTodos().stream().filter(Convenio::isAtivo).toList();}
    public UsuarioConvenio salvarConvenio(Long usuarioId,UsuarioConvenio x){if(x==null||x.getConvenioId()==null||x.getNumeroCarteirinha()==null||x.getNumeroCarteirinha().isBlank()||x.getTitular()==null||x.getTitular().isBlank()||x.getDataValidade()==null)throw new BusinessException("Plano, carteirinha, titular e validade são obrigatórios.");if(x.getDataValidade().isBefore(LocalDate.now()))throw new BusinessException("A carteirinha informada está vencida.");Convenio plano=convenioDao.buscarPorId(x.getConvenioId()).orElseThrow(()->new BusinessException("Plano não encontrado."));if(!plano.isAtivo())throw new BusinessException("Este plano não é aceito pela clínica.");x.setUsuarioId(usuarioId);return usuarioConvenioDao.salvar(x);}
}
