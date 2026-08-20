package br.com.orbe.service.impl;

import br.com.orbe.dao.UsuarioDao;
import br.com.orbe.dao.UsuarioPerfilDao;
import br.com.orbe.dto.UsuarioAutenticado;
import br.com.orbe.exception.AuthenticationException;
import br.com.orbe.exception.BusinessException;
import br.com.orbe.model.Usuario;
import br.com.orbe.model.enums.PerfilUsuario;
import br.com.orbe.model.enums.StatusUsuario;
import br.com.orbe.service.AutenticacaoService;
import br.com.orbe.util.PasswordHasher;

import java.util.LinkedHashSet;
import java.util.Set;

public class AutenticacaoServiceImpl implements AutenticacaoService {

    private final UsuarioDao usuarioDao;
    private final UsuarioPerfilDao perfilDao;

    public AutenticacaoServiceImpl(
            UsuarioDao usuarioDao,
            UsuarioPerfilDao perfilDao
    ) {
        this.usuarioDao = usuarioDao;
        this.perfilDao = perfilDao;
    }

    @Override
    public UsuarioAutenticado autenticar(
            String email,
            String senha,
            String enderecoIp
    ) {
        if (email == null || email.isBlank() || senha == null) {
            throw new BusinessException("E-mail e senha sao obrigatorios.");
        }

        Usuario usuario = usuarioDao.buscarPorEmail(email.trim().toLowerCase())
                .orElseThrow(this::credenciaisInvalidas);
        if (usuario.getStatus() != StatusUsuario.ATIVO
                || !PasswordHasher.matches(senha, usuario.getSenhaHash())) {
            throw credenciaisInvalidas();
        }

        Set<PerfilUsuario> perfis = new LinkedHashSet<>();
        perfilDao.listarPorUsuario(usuario.getId()).stream()
                .filter(perfil -> perfil.isAtivo())
                .forEach(perfil -> perfis.add(perfil.getPerfil()));
        if (perfis.isEmpty()) {
            throw new BusinessException("O usuario nao possui um perfil de acesso ativo.");
        }
        usuarioDao.registrarAcesso(usuario.getId(), enderecoIp);

        UsuarioAutenticado autenticado = new UsuarioAutenticado();
        autenticado.setId(usuario.getId());
        autenticado.setNome(usuario.getNome());
        autenticado.setEmail(usuario.getEmail());
        autenticado.setPerfis(Set.copyOf(perfis));
        autenticado.setTrocaSenhaObrigatoria(usuario.isTrocaSenhaObrigatoria());
        autenticado.setUnidade(usuario.getUnidade());
        return autenticado;
    }
    @Override public void alterarSenha(Long id,String atual,String novaSenha){Usuario u=usuarioDao.buscarPorId(id).orElseThrow(this::credenciaisInvalidas);if(!PasswordHasher.matches(atual,u.getSenhaHash()))throw credenciaisInvalidas();u.setSenhaHash(PasswordHasher.hash(novaSenha));u.setTrocaSenhaObrigatoria(false);usuarioDao.atualizar(u);}

    private AuthenticationException credenciaisInvalidas() {
        return new AuthenticationException("E-mail ou senha invalidos.");
    }
}
