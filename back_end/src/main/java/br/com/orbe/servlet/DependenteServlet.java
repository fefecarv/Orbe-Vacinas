package br.com.orbe.servlet;

import br.com.orbe.config.ApplicationContextListener;
import br.com.orbe.dto.ApiResponse;
import br.com.orbe.service.PortalPacienteService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/api/dependentes/*")
public class DependenteServlet extends BaseServlet {
    private PortalPacienteService service;

    @Override
    public void init() throws ServletException {
        service = (PortalPacienteService) getServletContext().getAttribute(
                ApplicationContextListener.PORTAL_PACIENTE_SERVICE
        );
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        var usuario = AutenticacaoServlet.usuarioDaSessao(request);
        json(response, HttpServletResponse.SC_OK,
                ApiResponse.ok(service.listarDependentes(usuario.getId())));
    }
}
