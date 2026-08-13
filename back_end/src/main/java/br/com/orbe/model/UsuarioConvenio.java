package br.com.orbe.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class UsuarioConvenio {
    private Long id;
    private Long usuarioId;
    private Long convenioId;
    private String numeroCarteirinha;
    private String titular;
    private LocalDate dataValidade;
    private LocalDateTime criadoEm;
    private String nomeConvenio;
    private String plano;

    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getUsuarioId(){return usuarioId;} public void setUsuarioId(Long v){usuarioId=v;}
    public Long getConvenioId(){return convenioId;} public void setConvenioId(Long v){convenioId=v;}
    public String getNumeroCarteirinha(){return numeroCarteirinha;} public void setNumeroCarteirinha(String v){numeroCarteirinha=v;}
    public String getTitular(){return titular;} public void setTitular(String v){titular=v;}
    public LocalDate getDataValidade(){return dataValidade;} public void setDataValidade(LocalDate v){dataValidade=v;}
    public LocalDateTime getCriadoEm(){return criadoEm;} public void setCriadoEm(LocalDateTime v){criadoEm=v;}
    public String getNomeConvenio(){return nomeConvenio;} public void setNomeConvenio(String v){nomeConvenio=v;}
    public String getPlano(){return plano;} public void setPlano(String v){plano=v;}
    public boolean isAtivo(){return dataValidade!=null&&!dataValidade.isBefore(LocalDate.now());}
}
