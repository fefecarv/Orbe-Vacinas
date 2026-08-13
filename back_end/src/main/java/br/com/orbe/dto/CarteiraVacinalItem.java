package br.com.orbe.dto;

import java.time.LocalDateTime;

public class CarteiraVacinalItem {

    private Long aplicacaoId;
    private String protocolo;
    private String vacina;
    private String fabricante;
    private String dose;
    private LocalDateTime dataAplicacao;
    private String numeroLote;
    private String localAplicacao;
    private String profissional;

    public Long getAplicacaoId() { return aplicacaoId; }
    public void setAplicacaoId(Long aplicacaoId) { this.aplicacaoId = aplicacaoId; }
    public String getProtocolo() { return protocolo; }
    public void setProtocolo(String protocolo) { this.protocolo = protocolo; }
    public String getVacina() { return vacina; }
    public void setVacina(String vacina) { this.vacina = vacina; }
    public String getFabricante() { return fabricante; }
    public void setFabricante(String fabricante) { this.fabricante = fabricante; }
    public String getDose() { return dose; }
    public void setDose(String dose) { this.dose = dose; }
    public LocalDateTime getDataAplicacao() { return dataAplicacao; }
    public void setDataAplicacao(LocalDateTime dataAplicacao) { this.dataAplicacao = dataAplicacao; }
    public String getNumeroLote() { return numeroLote; }
    public void setNumeroLote(String numeroLote) { this.numeroLote = numeroLote; }
    public String getLocalAplicacao() { return localAplicacao; }
    public void setLocalAplicacao(String localAplicacao) { this.localAplicacao = localAplicacao; }
    public String getProfissional() { return profissional; }
    public void setProfissional(String profissional) { this.profissional = profissional; }
}
