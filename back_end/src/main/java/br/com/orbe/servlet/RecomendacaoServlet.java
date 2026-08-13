package br.com.orbe.servlet;

import br.com.orbe.config.ApplicationContextListener;
import br.com.orbe.dto.ApiResponse;
import br.com.orbe.service.AutorizacaoService;
import br.com.orbe.service.PortalPacienteService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/api/recomendacoes/*")
public class RecomendacaoServlet extends BaseServlet {
    private PortalPacienteService service;
    private AutorizacaoService autorizacaoService;

    @Override
    public void init() throws ServletException {
        service = (PortalPacienteService) getServletContext().getAttribute(
                ApplicationContextListener.PORTAL_PACIENTE_SERVICE);
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
                    ApiResponse.ok(service.listarRecomendacoes(usuarioId, dependenteId)));
        } catch (Exception exception) {
            handleException(response, exception);
        }
    }
}
