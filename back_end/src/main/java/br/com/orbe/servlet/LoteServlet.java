package br.com.orbe.servlet;

import br.com.orbe.config.ApplicationContextListener;
import br.com.orbe.dto.ApiResponse;
import br.com.orbe.service.CatalogoService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/api/lotes/*")
public class LoteServlet extends BaseServlet {
    private CatalogoService service;

    @Override
    public void init() throws ServletException {
        service = (CatalogoService) getServletContext().getAttribute(
                ApplicationContextListener.CATALOGO_SERVICE
        );
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            Long vacinaId = optionalLongParameter(request, "vacinaId");
            if (vacinaId == null) throw new IllegalArgumentException("A vacina é obrigatória.");
            json(response, HttpServletResponse.SC_OK,
                    ApiResponse.ok(service.listarLotes(vacinaId)));
        } catch (Exception exception) {
            handleException(response, exception);
        }
    }
}
