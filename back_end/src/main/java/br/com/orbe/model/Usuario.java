package br.com.orbe.model;

import br.com.orbe.model.enums.StatusUsuario;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Usuario {

    private Long id;
    private String nome;
    private String cpf;
    private String email;
    private String senhaHash;
    private String telefone;
    private LocalDate dataNascimento;
    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String estado;
    private StatusUsuario status;
    private boolean verificacaoDuasEtapas;
    private boolean trocaSenhaObrigatoria;
    private String unidade;
    private LocalDateTime ultimoAcessoEm;
    private String ultimoIp;
    private String tokenRecuperacaoHash;
    private LocalDateTime tokenRecuperacaoExpiraEm;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public Usuario() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    @JsonIgnore
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }
    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }
    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getComplemento() { return complemento; }
    public void setComplemento(String complemento) { this.complemento = complemento; }
    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }
    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public StatusUsuario getStatus() { return status; }
    public void setStatus(StatusUsuario status) { this.status = status; }
    public boolean isVerificacaoDuasEtapas() { return verificacaoDuasEtapas; }
    public void setVerificacaoDuasEtapas(boolean verificacaoDuasEtapas) { this.verificacaoDuasEtapas = verificacaoDuasEtapas; }
    public boolean isTrocaSenhaObrigatoria(){return trocaSenhaObrigatoria;}public void setTrocaSenhaObrigatoria(boolean v){trocaSenhaObrigatoria=v;}public String getUnidade(){return unidade;}public void setUnidade(String v){unidade=v;}
    public LocalDateTime getUltimoAcessoEm() { return ultimoAcessoEm; }
    public void setUltimoAcessoEm(LocalDateTime ultimoAcessoEm) { this.ultimoAcessoEm = ultimoAcessoEm; }
    public String getUltimoIp() { return ultimoIp; }
    public void setUltimoIp(String ultimoIp) { this.ultimoIp = ultimoIp; }
    @JsonIgnore
    public String getTokenRecuperacaoHash() { return tokenRecuperacaoHash; }
    public void setTokenRecuperacaoHash(String tokenRecuperacaoHash) { this.tokenRecuperacaoHash = tokenRecuperacaoHash; }
    public LocalDateTime getTokenRecuperacaoExpiraEm() { return tokenRecuperacaoExpiraEm; }
    public void setTokenRecuperacaoExpiraEm(LocalDateTime tokenRecuperacaoExpiraEm) { this.tokenRecuperacaoExpiraEm = tokenRecuperacaoExpiraEm; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
