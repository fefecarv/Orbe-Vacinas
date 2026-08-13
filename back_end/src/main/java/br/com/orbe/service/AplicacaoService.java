package br.com.orbe.service;

import br.com.orbe.dto.RegistrarAplicacaoRequest;
import br.com.orbe.dto.CarteiraVacinalItem;
import br.com.orbe.model.Aplicacao;

import java.util.List;

public interface AplicacaoService {

    Aplicacao registrar(RegistrarAplicacaoRequest request);

    List<CarteiraVacinalItem> listarCarteira(Long usuarioId, Long dependenteId);
}
