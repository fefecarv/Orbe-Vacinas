package br.com.orbe.dao;

import br.com.orbe.model.UsuarioPerfil;

import java.util.List;

public interface UsuarioPerfilDao extends GenericDao<UsuarioPerfil, Long> {

    List<UsuarioPerfil> listarPorUsuario(Long usuarioId);

    boolean existeAdministrador();
}
