package br.com.orbe.dto;

import br.com.orbe.model.enums.PerfilUsuario;

import java.util.Set;
import java.io.Serializable;

public class UsuarioAutenticado implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String nome;
    private String email;
    private Set<PerfilUsuario> perfis;
    private String csrfToken;
    private boolean trocaSenhaObrigatoria;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<PerfilUsuario> getPerfis() {
        return perfis;
    }

    public void setPerfis(Set<PerfilUsuario> perfis) {
        this.perfis = perfis;
    }

    public String getCsrfToken() {
        return csrfToken;
    }

    public void setCsrfToken(String csrfToken) {
        this.csrfToken = csrfToken;
    }
    public boolean isTrocaSenhaObrigatoria(){return trocaSenhaObrigatoria;}public void setTrocaSenhaObrigatoria(boolean v){trocaSenhaObrigatoria=v;}
}
