package br.com.orbe.dto;

import br.com.orbe.model.enums.StatusAgendamento;

public class AtualizarStatusAgendamentoRequest {
    private StatusAgendamento status;
    public StatusAgendamento getStatus() { return status; }
    public void setStatus(StatusAgendamento status) { this.status = status; }
}
