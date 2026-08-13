package br.com.orbe.servlet;

import br.com.orbe.config.ApplicationContextListener;
import br.com.orbe.controller.AplicacaoController;
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

    private AplicacaoController controller;
    private AutorizacaoService autorizacaoService;

    @Override
    public void init() throws ServletException {
        AplicacaoService service = (AplicacaoService) getServletContext().getAttribute(
                ApplicationContextListener.APLICACAO_SERVICE);
        autorizacaoService = (AutorizacaoService) getServletContext().getAttribute(
                ApplicationContextListener.AUTORIZACAO_SERVICE);
        controller = new AplicacaoController(service);
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
                    controller.carteira(usuarioId, dependenteId));
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
            json(response, HttpServletResponse.SC_CREATED, controller.registrar(body));
        } catch (Exception exception) {
            handleException(response, exception);
        }
    }
}
