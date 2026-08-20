package br.com.orbe.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Vacina {

    private Long id;
    private String nome;
    private String fabricante;
    private String descricao;
    private String categoria;
    private String indicacao;
    private String esquemaDoses;
    private BigDecimal valorBase;
    private boolean ativo;
    private int idadeMinimaMeses;
    private Integer idadeMaximaMeses;
    private int numeroDoses;
    private Integer intervaloDias;
    private Integer reforcoMeses;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public Vacina() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getFabricante() { return fabricante; }
    public void setFabricante(String fabricante) { this.fabricante = fabricante; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getIndicacao() { return indicacao; }
    public void setIndicacao(String indicacao) { this.indicacao = indicacao; }
    public String getEsquemaDoses() { return esquemaDoses; }
    public void setEsquemaDoses(String esquemaDoses) { this.esquemaDoses = esquemaDoses; }
    public BigDecimal getValorBase() { return valorBase; }
    public void setValorBase(BigDecimal valorBase) { this.valorBase = valorBase; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public int getIdadeMinimaMeses(){return idadeMinimaMeses;} public void setIdadeMinimaMeses(int v){idadeMinimaMeses=v;}
    public Integer getIdadeMaximaMeses(){return idadeMaximaMeses;} public void setIdadeMaximaMeses(Integer v){idadeMaximaMeses=v;}
    public int getNumeroDoses(){return numeroDoses;} public void setNumeroDoses(int v){numeroDoses=v;}
    public Integer getIntervaloDias(){return intervaloDias;} public void setIntervaloDias(Integer v){intervaloDias=v;}
    public Integer getReforcoMeses(){return reforcoMeses;} public void setReforcoMeses(Integer v){reforcoMeses=v;}
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
