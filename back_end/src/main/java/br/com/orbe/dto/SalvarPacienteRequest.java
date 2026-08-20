package br.com.orbe.dto;

import java.time.LocalDate;

public class SalvarPacienteRequest {
    private String tipo;
    private String nome;
    private String cpf;
    private LocalDate dataNascimento;
    private String telefone;
    private String email;
    private String senhaTemporaria;
    private String status;
    private Long responsavelId;
    private String parentesco;
    private String cep, logradouro, numero, complemento, bairro, cidade, estado;

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenhaTemporaria() { return senhaTemporaria; }
    public void setSenhaTemporaria(String senhaTemporaria) { this.senhaTemporaria = senhaTemporaria; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getResponsavelId() { return responsavelId; }
    public void setResponsavelId(Long responsavelId) { this.responsavelId = responsavelId; }
    public String getParentesco() { return parentesco; }
    public void setParentesco(String parentesco) { this.parentesco = parentesco; }
    public String getCep(){return cep;} public void setCep(String v){cep=v;}
    public String getLogradouro(){return logradouro;} public void setLogradouro(String v){logradouro=v;}
    public String getNumero(){return numero;} public void setNumero(String v){numero=v;}
    public String getComplemento(){return complemento;} public void setComplemento(String v){complemento=v;}
    public String getBairro(){return bairro;} public void setBairro(String v){bairro=v;}
    public String getCidade(){return cidade;} public void setCidade(String v){cidade=v;}
    public String getEstado(){return estado;} public void setEstado(String v){estado=v;}
}
