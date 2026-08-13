package br.com.orbe.dao;

import br.com.orbe.model.Usuario;

import java.util.Optional;

public interface UsuarioDao extends GenericDao<Usuario, Long> {

    Optional<Usuario> buscarPorEmail(String email);

    Optional<Usuario> buscarPorCpf(String cpf);

    void registrarAcesso(Long usuarioId, String enderecoIp);
}
