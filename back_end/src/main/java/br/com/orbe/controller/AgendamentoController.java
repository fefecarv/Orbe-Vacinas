package br.com.orbe.controller;

import br.com.orbe.dto.ApiResponse;
import br.com.orbe.dto.CriarAgendamentoRequest;
import br.com.orbe.dto.AgendaDiariaItem;
import br.com.orbe.model.enums.StatusAgendamento;
import br.com.orbe.model.Agendamento;
import br.com.orbe.service.AgendamentoService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class AgendamentoController {

    private final AgendamentoService service;

    public AgendamentoController(AgendamentoService service) {
        this.service = service;
    }

    public ApiResponse<Agendamento> criar(CriarAgendamentoRequest request) {
        return ApiResponse.criado("Agendamento criado.", service.criar(request));
    }

    public Agendamento buscar(Long id) {
        return service.buscar(id);
    }

    public ApiResponse<Agendamento> cancelar(Long id, String motivo) {
        return ApiResponse.ok(service.cancelar(id, motivo));
    }

    public ApiResponse<Agendamento> reagendar(Long id, LocalDateTime data) {
        return ApiResponse.ok(service.reagendar(id, data));
    }

    public ApiResponse<List<Agendamento>> agenda(LocalDate data) {
        return ApiResponse.ok(service.listarAgenda(data));
    }

    public ApiResponse<List<Agendamento>> paciente(Long usuarioId, Long dependenteId) {
        return ApiResponse.ok(service.listarPaciente(usuarioId, dependenteId));
    }

    public ApiResponse<List<AgendaDiariaItem>> agendaDetalhada(LocalDate data) {
        return ApiResponse.ok(service.listarAgendaDetalhada(data));
    }

    public ApiResponse<Agendamento> atualizarStatus(Long id, StatusAgendamento status) {
        return ApiResponse.ok(service.atualizarStatus(id, status));
    }
}
