package br.com.orbe.dto;

import br.com.orbe.model.enums.StatusRecomendacao;

import java.time.LocalDate;

public class RecomendacaoVacinalItem {
    private Long id;
    private String vacina;
    private String dose;
    private LocalDate dataPrevista;
    private String motivo;
    private StatusRecomendacao status;
    private Long agendamentoId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getVacina() { return vacina; }
    public void setVacina(String vacina) { this.vacina = vacina; }
    public String getDose() { return dose; }
    public void setDose(String dose) { this.dose = dose; }
    public LocalDate getDataPrevista() { return dataPrevista; }
    public void setDataPrevista(LocalDate dataPrevista) { this.dataPrevista = dataPrevista; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public StatusRecomendacao getStatus() { return status; }
    public void setStatus(StatusRecomendacao status) { this.status = status; }
    public Long getAgendamentoId() { return agendamentoId; }
    public void setAgendamentoId(Long agendamentoId) { this.agendamentoId = agendamentoId; }
}
