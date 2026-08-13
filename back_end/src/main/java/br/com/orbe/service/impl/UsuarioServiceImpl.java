package br.com.orbe.service.impl;

import br.com.orbe.dao.UsuarioDao;
import br.com.orbe.dao.jdbc.UsuarioCadastroDaoJdbc;
import br.com.orbe.exception.BusinessException;
import br.com.orbe.exception.NotFoundException;
import br.com.orbe.model.Usuario;
import br.com.orbe.model.UsuarioPerfil;
import br.com.orbe.service.UsuarioService;

import java.util.List;
import java.time.LocalDate;

public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioDao usuarioDao;
    private final UsuarioCadastroDaoJdbc cadastroDao;

    public UsuarioServiceImpl(
            UsuarioDao usuarioDao,
            UsuarioCadastroDaoJdbc cadastroDao
    ) {
        this.usuarioDao = usuarioDao;
        this.cadastroDao = cadastroDao;
    }

    @Override
    public Usuario cadastrar(Usuario usuario, UsuarioPerfil perfil) {
        validar(usuario);
        usuario.setEmail(usuario.getEmail().trim().toLowerCase());
        if (usuarioDao.buscarPorEmail(usuario.getEmail()).isPresent()) {
            throw new BusinessException("Já existe um usuário com este e-mail.");
        }
        if (usuarioDao.buscarPorCpf(usuario.getCpf()).isPresent()) {
            throw new BusinessException("Já existe um usuário com este CPF.");
        }
        if (perfil == null || perfil.getPerfil() == null) {
            throw new BusinessException("O perfil de acesso é obrigatório.");
        }
        perfil.setAtivo(true);
        return cadastroDao.cadastrarComPerfil(usuario, perfil);
    }

    @Override
    public Usuario buscar(Long id) {
        return usuarioDao.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
    }

    @Override
    public List<Usuario> listar() {
        return usuarioDao.listarTodos();
    }

    @Override
    public Usuario atualizar(Long id, Usuario usuario) {
        buscar(id);
        validar(usuario);
        usuario.setId(id);
        return usuarioDao.atualizar(usuario);
    }

    private void validar(Usuario usuario) {
        if (usuario == null) {
            throw new BusinessException("Os dados do usuário são obrigatórios.");
        }
        if (usuario.getNome() == null || usuario.getNome().isBlank()) {
            throw new BusinessException("O nome é obrigatório.");
        }
        if (usuario.getCpf() == null || !usuario.getCpf().matches("\\d{11}")) {
            throw new BusinessException("O CPF deve possuir 11 números.");
        }
        if (usuario.getEmail() == null || !usuario.getEmail().contains("@")) {
            throw new BusinessException("O e-mail informado é inválido.");
        }
        if (usuario.getTelefone() == null || usuario.getTelefone().isBlank()) {
            throw new BusinessException("O telefone é obrigatório.");
        }
        if (usuario.getDataNascimento() == null
                || !usuario.getDataNascimento().isBefore(LocalDate.now())) {
            throw new BusinessException("Informe uma data de nascimento válida.");
        }
        if (usuario.getSenhaHash() == null || usuario.getSenhaHash().isBlank()) {
            throw new BusinessException("A senha protegida é obrigatória.");
        }
    }
}
