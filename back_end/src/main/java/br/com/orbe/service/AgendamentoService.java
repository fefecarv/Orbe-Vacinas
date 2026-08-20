package br.com.orbe.service;

import br.com.orbe.dto.CriarAgendamentoRequest;
import br.com.orbe.dto.AgendaDiariaItem;
import br.com.orbe.dto.AnaliseConvenio;
import br.com.orbe.model.enums.StatusAgendamento;
import br.com.orbe.model.Agendamento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public interface AgendamentoService {

    Agendamento criar(CriarAgendamentoRequest request);

    AnaliseConvenio analisarConvenio(Long vacinaId, Long convenioId);

    Agendamento buscar(Long id);

    Agendamento cancelar(Long id, String motivo);

    Agendamento reagendar(Long id, LocalDateTime novaData);

    List<Agendamento> listarAgenda(LocalDate data);

    List<Agendamento> listarPaciente(Long usuarioId, Long dependenteId);

    List<AgendaDiariaItem> listarAgendaDetalhada(LocalDate data);

    Agendamento atualizarStatus(Long id, StatusAgendamento status);
    List<LocalTime> horariosDisponiveis(LocalDate data, String unidade);
}
