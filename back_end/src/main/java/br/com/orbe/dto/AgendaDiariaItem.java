package br.com.orbe.dto;

import br.com.orbe.model.enums.StatusAgendamento;
import br.com.orbe.model.enums.TipoAtendimento;

import java.time.LocalDateTime;

public class AgendaDiariaItem {
    private Long id;
    private Long usuarioId;
    private Long dependenteId;
    private String paciente;
    private String cpf;
    private String vacina;
    private Long vacinaId;
    private String dose;
    private LocalDateTime dataAgendamento;
    private String sala;
    private StatusAgendamento status;
    private TipoAtendimento tipoAtendimento;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public Long getDependenteId() { return dependenteId; }
    public void setDependenteId(Long dependenteId) { this.dependenteId = dependenteId; }
    public String getPaciente() { return paciente; }
    public void setPaciente(String paciente) { this.paciente = paciente; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getVacina() { return vacina; }
    public void setVacina(String vacina) { this.vacina = vacina; }
    public Long getVacinaId() { return vacinaId; }
    public void setVacinaId(Long vacinaId) { this.vacinaId = vacinaId; }
    public String getDose() { return dose; }
    public void setDose(String dose) { this.dose = dose; }
    public LocalDateTime getDataAgendamento() { return dataAgendamento; }
    public void setDataAgendamento(LocalDateTime dataAgendamento) { this.dataAgendamento = dataAgendamento; }
    public String getSala() { return sala; }
    public void setSala(String sala) { this.sala = sala; }
    public StatusAgendamento getStatus() { return status; }
    public void setStatus(StatusAgendamento status) { this.status = status; }
    public TipoAtendimento getTipoAtendimento() { return tipoAtendimento; }
    public void setTipoAtendimento(TipoAtendimento tipoAtendimento) { this.tipoAtendimento = tipoAtendimento; }
}
