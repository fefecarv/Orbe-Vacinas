package br.com.orbe.service;
import br.com.orbe.dto.PacienteResumo;
import br.com.orbe.dto.SalvarPacienteRequest;
import java.util.List;
public interface PacienteService {
    List<PacienteResumo> listar();
    PacienteResumo cadastrar(SalvarPacienteRequest request);
    PacienteResumo atualizar(String id, SalvarPacienteRequest request);
}
