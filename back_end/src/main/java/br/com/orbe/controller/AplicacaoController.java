package br.com.orbe.controller;

import br.com.orbe.dto.ApiResponse;
import br.com.orbe.dto.RegistrarAplicacaoRequest;
import br.com.orbe.dto.CarteiraVacinalItem;
import br.com.orbe.model.Aplicacao;
import br.com.orbe.service.AplicacaoService;

import java.util.List;

public final class AplicacaoController {

    private final AplicacaoService service;

    public AplicacaoController(AplicacaoService service) {
        this.service = service;
    }

    public ApiResponse<Aplicacao> registrar(RegistrarAplicacaoRequest request) {
        return ApiResponse.criado("Aplicação registrada.", service.registrar(request));
    }

    public ApiResponse<List<CarteiraVacinalItem>> carteira(
            Long usuarioId,
            Long dependenteId
    ) {
        return ApiResponse.ok(service.listarCarteira(usuarioId, dependenteId));
    }
}
