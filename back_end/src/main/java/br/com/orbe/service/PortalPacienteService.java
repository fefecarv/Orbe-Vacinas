package br.com.orbe.service;

import br.com.orbe.dto.RecomendacaoVacinalItem;
import br.com.orbe.model.Dependente;
import br.com.orbe.model.Convenio;
import br.com.orbe.model.UsuarioConvenio;

import java.util.List;

public interface PortalPacienteService {
    List<Dependente> listarDependentes(Long usuarioId);
    List<RecomendacaoVacinalItem> listarRecomendacoes(Long usuarioId, Long dependenteId);
    List<UsuarioConvenio> listarConvenios(Long usuarioId);
    List<Convenio> listarConveniosAceitos();
    UsuarioConvenio salvarConvenio(Long usuarioId, UsuarioConvenio convenio);
}
