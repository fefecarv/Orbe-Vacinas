package br.com.orbe.model;

import br.com.orbe.model.enums.TipoCobertura;

import java.math.BigDecimal;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Convenio {

    private Long id;
    private String nome;
    private String plano;
    private String codigoOperacional;
    private boolean ativo;
    private TipoCobertura tipoCobertura;
    private BigDecimal percentualDesconto;
    private BigDecimal valorCoparticipacao;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public Convenio() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getPlano() { return plano; }
    public void setPlano(String plano) { this.plano = plano; }
    public String getCodigoOperacional() { return codigoOperacional; }
    public void setCodigoOperacional(String codigoOperacional) { this.codigoOperacional = codigoOperacional; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public TipoCobertura getTipoCobertura() { return tipoCobertura; }
    public void setTipoCobertura(TipoCobertura tipoCobertura) { this.tipoCobertura = tipoCobertura; }
    public BigDecimal getPercentualDesconto() { return percentualDesconto; }
    public void setPercentualDesconto(BigDecimal percentualDesconto) { this.percentualDesconto = percentualDesconto; }
    public BigDecimal getValorCoparticipacao() { return valorCoparticipacao; }
    public void setValorCoparticipacao(BigDecimal valorCoparticipacao) { this.valorCoparticipacao = valorCoparticipacao; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
