package br.com.orbe.service;

import br.com.orbe.dto.UsuarioAutenticado;

public interface AutorizacaoService {

    void validarAgendamento(
            UsuarioAutenticado autenticado,
            Long usuarioId,
            Long dependenteId
    );

    void validarCarteira(
            UsuarioAutenticado autenticado,
            Long usuarioId,
            Long dependenteId
    );

    void validarConvenio(UsuarioAutenticado autenticado, Long convenioId);
}
