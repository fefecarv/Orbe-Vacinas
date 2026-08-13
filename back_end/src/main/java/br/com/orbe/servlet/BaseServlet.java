package br.com.orbe.servlet;

import br.com.orbe.dto.ApiResponse;
import br.com.orbe.exception.BusinessException;
import br.com.orbe.exception.AuthenticationException;
import br.com.orbe.exception.ForbiddenException;
import br.com.orbe.exception.NotFoundException;
import br.com.orbe.exception.PersistenceException;
import br.com.orbe.util.JsonUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public abstract class BaseServlet extends HttpServlet {

    protected void json(
            HttpServletResponse response,
            int status,
            Object body
    ) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        JsonUtil.mapper().writeValue(response.getWriter(), body);
    }

    protected void error(
            HttpServletResponse response,
            int status,
            String message
    ) throws IOException {
        json(response, status, new ApiResponse<>(false, message, null));
    }

    protected void handleException(
            HttpServletResponse response,
            Exception exception
    ) throws IOException {
        if (exception instanceof AuthenticationException) {
            error(response, HttpServletResponse.SC_UNAUTHORIZED, exception.getMessage());
            return;
        }
        if (exception instanceof ForbiddenException) {
            error(response, HttpServletResponse.SC_FORBIDDEN, exception.getMessage());
            return;
        }
        if (exception instanceof NotFoundException) {
            error(response, HttpServletResponse.SC_NOT_FOUND, exception.getMessage());
            return;
        }
        if (exception instanceof BusinessException
                || exception instanceof IllegalArgumentException) {
            error(response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
            return;
        }
        if (exception instanceof PersistenceException) {
            error(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Nao foi possivel concluir a operacao."
            );
            return;
        }
        error(response, HttpServletResponse.SC_BAD_REQUEST, "Requisicao invalida.");
    }

    protected Long pathId(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path == null || path.equals("/")) {
            return null;
        }
        String firstSegment = path.substring(1).split("/")[0];
        try {
            return Long.valueOf(firstSegment);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Identificador invalido.");
        }
    }

    protected String pathAction(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path == null) {
            return null;
        }
        String[] segments = path.substring(1).split("/");
        return segments.length > 1 ? segments[1] : null;
    }

    protected Long optionalLongParameter(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Parametro '" + name + "' invalido.");
        }
    }
}
