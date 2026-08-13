package br.com.orbe.service.impl;

import br.com.orbe.dao.UsuarioDependenteDao;
import br.com.orbe.dao.jdbc.UsuarioConvenioDaoJdbc;
import br.com.orbe.dto.UsuarioAutenticado;
import br.com.orbe.exception.ForbiddenException;
import br.com.orbe.model.UsuarioDependente;
import br.com.orbe.model.enums.PerfilUsuario;
import br.com.orbe.service.AutorizacaoService;

import java.util.Objects;
import java.util.function.Predicate;

public class AutorizacaoServiceImpl implements AutorizacaoService {

    private final UsuarioDependenteDao vinculoDao;
    private final UsuarioConvenioDaoJdbc convenioDao;

    public AutorizacaoServiceImpl(UsuarioDependenteDao vinculoDao, UsuarioConvenioDaoJdbc convenioDao) {
        this.vinculoDao = vinculoDao;
        this.convenioDao = convenioDao;
    }

    @Override
    public void validarConvenio(UsuarioAutenticado autenticado, Long convenioId) {
        if (convenioId == null || possuiPerfilInterno(autenticado)) {
            return;
        }
        boolean pertenceAoUsuario = convenioDao.listarPorUsuario(autenticado.getId()).stream()
                .anyMatch(convenio -> convenio.isAtivo()
                        && java.util.Objects.equals(convenio.getId(), convenioId));
        if (!pertenceAoUsuario) {
            throw new ForbiddenException("Voce nao possui permissao para utilizar este convenio.");
        }
    }

    @Override
    public void validarAgendamento(
            UsuarioAutenticado autenticado,
            Long usuarioId,
            Long dependenteId
    ) {
        validarPaciente(
                autenticado,
                usuarioId,
                dependenteId,
                UsuarioDependente::isPodeAgendar
        );
    }

    @Override
    public void validarCarteira(
            UsuarioAutenticado autenticado,
            Long usuarioId,
            Long dependenteId
    ) {
        validarPaciente(
                autenticado,
                usuarioId,
                dependenteId,
                UsuarioDependente::isPodeVisualizarCarteira
        );
    }

    private void validarPaciente(
            UsuarioAutenticado autenticado,
            Long usuarioId,
            Long dependenteId,
            Predicate<UsuarioDependente> permissao
    ) {
        if (possuiPerfilInterno(autenticado)) {
            return;
        }
        if (usuarioId != null && Objects.equals(usuarioId, autenticado.getId())) {
            return;
        }
        boolean vinculoPermitido = dependenteId != null
                && vinculoDao.listarPorUsuario(autenticado.getId()).stream()
                .anyMatch(vinculo -> Objects.equals(vinculo.getDependenteId(), dependenteId)
                        && permissao.test(vinculo));
        if (!vinculoPermitido) {
            throw new ForbiddenException("Voce nao possui permissao para acessar este paciente.");
        }
    }

    private boolean possuiPerfilInterno(UsuarioAutenticado autenticado) {
        return autenticado.getPerfis().contains(PerfilUsuario.FUNCIONARIO)
                || autenticado.getPerfis().contains(PerfilUsuario.ADMINISTRADOR);
    }
}
