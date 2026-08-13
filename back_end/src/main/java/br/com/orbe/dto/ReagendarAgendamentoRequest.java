package br.com.orbe.dto;

import java.time.LocalDateTime;

public class ReagendarAgendamentoRequest {

    private LocalDateTime novaData;

    public LocalDateTime getNovaData() {
        return novaData;
    }

    public void setNovaData(LocalDateTime novaData) {
        this.novaData = novaData;
    }
}
