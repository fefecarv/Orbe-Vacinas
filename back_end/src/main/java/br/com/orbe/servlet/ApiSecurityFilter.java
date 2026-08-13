package br.com.orbe.servlet;

import br.com.orbe.dto.ApiResponse;
import br.com.orbe.dto.UsuarioAutenticado;
import br.com.orbe.model.enums.PerfilUsuario;
import br.com.orbe.util.JsonUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebFilter("/api/*")
public class ApiSecurityFilter implements Filter {

    @Override
    public void doFilter(
            ServletRequest servletRequest,
            ServletResponse servletResponse,
            FilterChain chain
    ) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        addCorsHeaders(request, response);
        if (request.getMethod().equals("OPTIONS")) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }

        if (isPublic(request)) {
            chain.doFilter(request, response);
            return;
        }

        UsuarioAutenticado usuario = AutenticacaoServlet.usuarioDaSessao(request);
        if (usuario == null) {
            reject(response, HttpServletResponse.SC_UNAUTHORIZED, "Autenticacao obrigatoria.");
            return;
        }
        if (!roleAllowed(request, usuario)) {
            reject(response, HttpServletResponse.SC_FORBIDDEN, "Acesso nao autorizado.");
            return;
        }
        if (isMutation(request) && !validCsrf(request, usuario)) {
            reject(response, HttpServletResponse.SC_FORBIDDEN, "Token CSRF invalido.");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isPublic(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return (path.equals("/api/auth/login") && request.getMethod().equals("POST"))
                || (path.equals("/api/usuarios") && request.getMethod().equals("POST"));
    }

    private boolean roleAllowed(
            HttpServletRequest request,
            UsuarioAutenticado usuario
    ) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String method = request.getMethod();
        if (path.startsWith("/api/aplicacoes") && method.equals("POST")) {
            return internalUser(usuario);
        }
        if (path.startsWith("/api/lotes")) {
            return internalUser(usuario);
        }
        if (path.startsWith("/api/pacientes")) {
            return internalUser(usuario);
        }
        if (path.startsWith("/api/admin/")) {
            return usuario.getPerfis().contains(PerfilUsuario.ADMINISTRADOR);
        }
        if (path.startsWith("/api/usuarios") && !method.equals("POST")) {
            return usuario.getPerfis().contains(PerfilUsuario.ADMINISTRADOR);
        }
        if (path.startsWith("/api/vacinas") && !method.equals("GET")) {
            return usuario.getPerfis().contains(PerfilUsuario.ADMINISTRADOR);
        }
        return true;
    }

    private boolean internalUser(UsuarioAutenticado usuario) {
        return usuario.getPerfis().contains(PerfilUsuario.FUNCIONARIO)
                || usuario.getPerfis().contains(PerfilUsuario.ADMINISTRADOR);
    }

    private boolean isMutation(HttpServletRequest request) {
        return !request.getMethod().equals("GET")
                && !request.getMethod().equals("HEAD");
    }

    private boolean validCsrf(
            HttpServletRequest request,
            UsuarioAutenticado usuario
    ) {
        return usuario.getCsrfToken() != null
                && usuario.getCsrfToken().equals(request.getHeader("X-CSRF-Token"));
    }

    private void addCorsHeaders(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String allowedOrigin = System.getenv().getOrDefault(
                "ORBE_ALLOWED_ORIGIN",
                "http://localhost:5173"
        );
        String origin = request.getHeader("Origin");
        if (allowedOrigin.equals(origin)) {
            response.setHeader("Access-Control-Allow-Origin", allowedOrigin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Vary", "Origin");
        }
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, X-CSRF-Token");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
    }

    private void reject(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        JsonUtil.mapper().writeValue(
                response.getWriter(),
                new ApiResponse<>(false, message, null)
        );
    }
}
