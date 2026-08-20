package br.com.orbe.servlet;

import br.com.orbe.config.ApplicationContextListener;
import br.com.orbe.dto.CadastroUsuarioRequest;
import br.com.orbe.dto.ApiResponse;
import br.com.orbe.model.Usuario;
import br.com.orbe.model.UsuarioPerfil;
import br.com.orbe.model.enums.PerfilUsuario;
import br.com.orbe.model.enums.StatusUsuario;
import br.com.orbe.service.UsuarioService;
import br.com.orbe.util.JsonUtil;
import br.com.orbe.util.PasswordHasher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/api/usuarios/*")
public class UsuarioServlet extends BaseServlet {

    private UsuarioService service;

    @Override
    public void init() throws ServletException {
        service = (UsuarioService) getServletContext().getAttribute(
                ApplicationContextListener.USUARIO_SERVICE
        );
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            Long id = pathId(request);
            json(response, HttpServletResponse.SC_OK,
                    id == null ? ApiResponse.ok(service.listar()) : ApiResponse.ok(service.buscar(id)));
        } catch (Exception exception) {
            handleException(response, exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            CadastroUsuarioRequest body = JsonUtil.mapper().readValue(
                    request.getReader(), CadastroUsuarioRequest.class);
            Usuario usuario = body.getUsuario();
            if (usuario == null) {
                throw new IllegalArgumentException("Os dados do usuario sao obrigatorios.");
            }
            usuario.setSenhaHash(PasswordHasher.hash(body.getSenha()));
            usuario.setStatus(StatusUsuario.ATIVO);

            UsuarioPerfil perfil = body.getPerfil();
            if (perfil == null) {
                perfil = new UsuarioPerfil();
            }
            if (!isAdministrator(request)) {
                perfil.setPerfil(PerfilUsuario.PACIENTE);
                perfil.setMatricula(null);
                perfil.setCargo(null);
            }
            perfil.setAtivo(true);

            json(response, HttpServletResponse.SC_CREATED,
                    ApiResponse.criado("Usuário cadastrado.", service.cadastrar(usuario, perfil)));
        } catch (Exception exception) {
            handleException(response, exception);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            Long id = pathId(request);
            if (id == null) {
                throw new IllegalArgumentException("Informe o usuario que sera atualizado.");
            }
            Usuario usuario = JsonUtil.mapper().readValue(request.getReader(), Usuario.class);
            Usuario atual = service.buscar(id);
            usuario.setSenhaHash(atual.getSenhaHash());
            json(response, HttpServletResponse.SC_OK, ApiResponse.ok(service.atualizar(id, usuario)));
        } catch (Exception exception) {
            handleException(response, exception);
        }
    }

    private boolean isAdministrator(HttpServletRequest request) {
        var autenticado = AutenticacaoServlet.usuarioDaSessao(request);
        return autenticado != null
                && autenticado.getPerfis().contains(PerfilUsuario.ADMINISTRADOR);
    }
}
