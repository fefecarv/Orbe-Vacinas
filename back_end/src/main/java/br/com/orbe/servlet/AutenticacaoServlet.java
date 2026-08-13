package br.com.orbe.servlet;

import br.com.orbe.config.ApplicationContextListener;
import br.com.orbe.dto.ApiResponse;
import br.com.orbe.dto.LoginRequest;
import br.com.orbe.dto.UsuarioAutenticado;
import br.com.orbe.service.AutenticacaoService;
import br.com.orbe.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.UUID;

@WebServlet("/api/auth/*")
public class AutenticacaoServlet extends BaseServlet {

    public static final String USUARIO_AUTENTICADO = "usuarioAutenticado";

    private AutenticacaoService service;

    @Override
    public void init() throws ServletException {
        service = (AutenticacaoService) getServletContext().getAttribute(
                ApplicationContextListener.AUTENTICACAO_SERVICE
        );
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        UsuarioAutenticado usuario = usuarioDaSessao(request);
        if (usuario == null) {
            error(response, HttpServletResponse.SC_UNAUTHORIZED, "Sessao nao autenticada.");
            return;
        }
        json(response, HttpServletResponse.SC_OK, ApiResponse.ok(usuario));
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        String action = pathActionName(request);
        if ("logout".equals(action)) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            json(response, HttpServletResponse.SC_OK, ApiResponse.ok(null));
            return;
        }
        if (!"login".equals(action)) {
            error(response, HttpServletResponse.SC_NOT_FOUND, "Rota de autenticacao inexistente.");
            return;
        }

        try {
            LoginRequest body = JsonUtil.mapper().readValue(request.getReader(), LoginRequest.class);
            UsuarioAutenticado usuario = service.autenticar(
                    body.getEmail(),
                    body.getSenha(),
                    request.getRemoteAddr()
            );
            usuario.setCsrfToken(UUID.randomUUID().toString());
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            request.getSession(true).setAttribute(USUARIO_AUTENTICADO, usuario);
            json(response, HttpServletResponse.SC_OK, ApiResponse.ok(usuario));
        } catch (Exception exception) {
            handleException(response, exception);
        }
    }

    public static UsuarioAutenticado usuarioDaSessao(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null
                ? null
                : (UsuarioAutenticado) session.getAttribute(USUARIO_AUTENTICADO);
    }

    private String pathActionName(HttpServletRequest request) {
        String path = request.getPathInfo();
        return path == null ? "" : path.replace("/", "");
    }
}
