package br.com.orbe.controller;

import br.com.orbe.dto.ApiResponse;
import br.com.orbe.model.Usuario;
import br.com.orbe.model.UsuarioPerfil;
import br.com.orbe.service.UsuarioService;

import java.util.List;

public final class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    public ApiResponse<Usuario> cadastrar(
            Usuario usuario,
            UsuarioPerfil perfil
    ) {
        return ApiResponse.criado(
                "Usuário cadastrado.",
                service.cadastrar(usuario, perfil)
        );
    }

    public ApiResponse<List<Usuario>> listar() {
        return ApiResponse.ok(service.listar());
    }

    public ApiResponse<Usuario> buscar(Long id) {
        return ApiResponse.ok(service.buscar(id));
    }

    public ApiResponse<Usuario> atualizar(Long id, Usuario usuario) {
        return ApiResponse.ok(service.atualizar(id, usuario));
    }
}
