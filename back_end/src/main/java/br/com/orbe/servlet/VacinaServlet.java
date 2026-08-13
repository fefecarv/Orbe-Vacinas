package br.com.orbe.servlet;

import br.com.orbe.config.ApplicationContextListener;
import br.com.orbe.dto.ApiResponse;
import br.com.orbe.model.Vacina;
import br.com.orbe.service.CatalogoService;
import br.com.orbe.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/api/vacinas/*")
public class VacinaServlet extends BaseServlet {

    private CatalogoService service;

    @Override
    public void init() throws ServletException {
        service = (CatalogoService) getServletContext().getAttribute(
                ApplicationContextListener.CATALOGO_SERVICE
        );
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        json(response, HttpServletResponse.SC_OK, ApiResponse.ok(
                service.listarVacinasDisponiveis()
        ));
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        try {
            Vacina vacina = JsonUtil.mapper().readValue(request.getReader(), Vacina.class);
            Vacina criada = service.salvarVacina(vacina);
            json(
                    response,
                    HttpServletResponse.SC_CREATED,
                    ApiResponse.criado("Vacina cadastrada.", criada)
            );
        } catch (Exception exception) {
            handleException(response, exception);
        }
    }

    @Override
    protected void doPut(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        try {
            Vacina vacina = JsonUtil.mapper().readValue(request.getReader(), Vacina.class);
            json(response, HttpServletResponse.SC_OK, ApiResponse.ok(
                    service.salvarVacina(vacina)
            ));
        } catch (Exception exception) {
            handleException(response, exception);
        }
    }
}
