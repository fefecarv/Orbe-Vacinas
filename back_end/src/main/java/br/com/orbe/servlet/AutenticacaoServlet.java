package br.com.orbe.servlet;

import br.com.orbe.config.ApplicationContextListener;
import br.com.orbe.dto.ApiResponse;
import br.com.orbe.dto.LoginRequest;
import br.com.orbe.dto.UsuarioAutenticado;
import br.com.orbe.model.Usuario;
import br.com.orbe.service.AutenticacaoService;
import br.com.orbe.service.UsuarioService;
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
    private UsuarioService usuarioService;

    @Override
    public void init() throws ServletException {
        service = (AutenticacaoService) getServletContext().getAttribute(
                ApplicationContextListener.AUTENTICACAO_SERVICE
        );
        usuarioService = (UsuarioService) getServletContext().getAttribute(
                ApplicationContextListener.USUARIO_SERVICE
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
        if ("perfil".equals(pathActionName(request))) {
            json(response, HttpServletResponse.SC_OK,
                    ApiResponse.ok(usuarioService.buscar(usuario.getId())));
            return;
        }
        json(response, HttpServletResponse.SC_OK, ApiResponse.ok(usuario));
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            if (!"perfil".equals(pathActionName(request))) {
                error(response, HttpServletResponse.SC_NOT_FOUND,
                        "Rota de autenticacao inexistente.");
                return;
            }
            UsuarioAutenticado autenticado = usuarioDaSessao(request);
            Usuario alteracoes = JsonUtil.mapper().readValue(request.getReader(), Usuario.class);
            Usuario atual = usuarioService.buscar(autenticado.getId());

            atual.setNome(alteracoes.getNome());
            atual.setEmail(alteracoes.getEmail());
            atual.setTelefone(alteracoes.getTelefone());
            atual.setDataNascimento(alteracoes.getDataNascimento());
            atual.setCep(alteracoes.getCep());
            atual.setLogradouro(alteracoes.getLogradouro());
            atual.setNumero(alteracoes.getNumero());
            atual.setComplemento(alteracoes.getComplemento());
            atual.setBairro(alteracoes.getBairro());
            atual.setCidade(alteracoes.getCidade());
            atual.setEstado(alteracoes.getEstado());

            Usuario atualizado = usuarioService.atualizar(autenticado.getId(), atual);
            autenticado.setNome(atualizado.getNome());
            autenticado.setEmail(atualizado.getEmail());
            json(response, HttpServletResponse.SC_OK, ApiResponse.ok(atualizado));
        } catch (Exception exception) {
            handleException(response, exception);
        }
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
        if("alterar-senha".equals(action)){try{UsuarioAutenticado u=usuarioDaSessao(request);var body=JsonUtil.mapper().readTree(request.getReader());service.alterarSenha(u.getId(),body.path("senhaAtual").asText(),body.path("novaSenha").asText());u.setTrocaSenhaObrigatoria(false);json(response,HttpServletResponse.SC_OK,ApiResponse.ok(null));}catch(Exception e){handleException(response,e);}return;}
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
