package br.com.orbe.service.impl;

import br.com.orbe.config.AdminBootstrapConfig;
import br.com.orbe.dao.UsuarioPerfilDao;
import br.com.orbe.model.Usuario;
import br.com.orbe.model.UsuarioPerfil;
import br.com.orbe.model.enums.PerfilUsuario;
import br.com.orbe.model.enums.StatusUsuario;
import br.com.orbe.service.AdminBootstrapService;
import br.com.orbe.service.UsuarioService;
import br.com.orbe.util.PasswordHasher;

public class AdminBootstrapServiceImpl implements AdminBootstrapService {

    private final UsuarioService usuarioService;
    private final UsuarioPerfilDao perfilDao;

    public AdminBootstrapServiceImpl(
            UsuarioService usuarioService,
            UsuarioPerfilDao perfilDao
    ) {
        this.usuarioService = usuarioService;
        this.perfilDao = perfilDao;
    }

    @Override
    public boolean criarPrimeiroAdministrador(AdminBootstrapConfig config) {
        if (!config.isEnabled() || perfilDao.existeAdministrador()) {
            return false;
        }

        Usuario usuario = new Usuario();
        usuario.setNome(config.getName());
        usuario.setCpf(config.getCpf());
        usuario.setEmail(config.getEmail());
        usuario.setSenhaHash(PasswordHasher.hash(config.getPassword()));
        usuario.setTelefone(config.getPhone());
        usuario.setDataNascimento(config.getBirthDate());
        usuario.setStatus(StatusUsuario.ATIVO);

        UsuarioPerfil perfil = new UsuarioPerfil();
        perfil.setPerfil(PerfilUsuario.ADMINISTRADOR);
        perfil.setCargo("Administrador do sistema");
        perfil.setAtivo(true);

        usuarioService.cadastrar(usuario, perfil);
        return true;
    }
}
