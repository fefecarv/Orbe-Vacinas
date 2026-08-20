package br.com.orbe.servlet;

import br.com.orbe.config.ApplicationContextListener;
import br.com.orbe.dto.ApiResponse;
import br.com.orbe.dto.RegistrarAplicacaoRequest;
import br.com.orbe.exception.ForbiddenException;
import br.com.orbe.service.AplicacaoService;
import br.com.orbe.service.AutorizacaoService;
import br.com.orbe.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/api/aplicacoes/*")
public class AplicacaoServlet extends BaseServlet {

    private AplicacaoService service;
    private AutorizacaoService autorizacaoService;

    @Override
    public void init() throws ServletException {
        service = (AplicacaoService) getServletContext().getAttribute(
                ApplicationContextListener.APLICACAO_SERVICE);
        autorizacaoService = (AutorizacaoService) getServletContext().getAttribute(
                ApplicationContextListener.AUTORIZACAO_SERVICE);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            Long usuarioId = optionalLongParameter(request, "usuarioId");
            Long dependenteId = optionalLongParameter(request, "dependenteId");
            autorizacaoService.validarCarteira(
                    AutenticacaoServlet.usuarioDaSessao(request), usuarioId, dependenteId);
            json(response, HttpServletResponse.SC_OK,
                    ApiResponse.ok(service.listarCarteira(usuarioId, dependenteId)));
        } catch (Exception exception) {
            handleException(response, exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            RegistrarAplicacaoRequest body = JsonUtil.mapper().readValue(
                    request.getReader(), RegistrarAplicacaoRequest.class);
            var autenticado = AutenticacaoServlet.usuarioDaSessao(request);
            if (!autenticado.getId().equals(body.funcionarioId())) {
                throw new ForbiddenException(
                        "O funcionario da aplicacao deve ser o usuario autenticado.");
            }
            json(response, HttpServletResponse.SC_CREATED,
                    ApiResponse.criado("Aplicação registrada.", service.registrar(body)));
        } catch (Exception exception) {
            handleException(response, exception);
        }
    }
}
