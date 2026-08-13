package br.com.orbe.dto;

import br.com.orbe.model.Usuario;
import br.com.orbe.model.UsuarioPerfil;

public class CadastroUsuarioRequest {

    private Usuario usuario;
    private UsuarioPerfil perfil;
    private String senha;

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public UsuarioPerfil getPerfil() {
        return perfil;
    }

    public void setPerfil(UsuarioPerfil perfil) {
        this.perfil = perfil;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }
}
