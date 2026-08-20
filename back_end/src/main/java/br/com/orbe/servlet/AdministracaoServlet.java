package br.com.orbe.servlet;

import br.com.orbe.config.ApplicationContextListener;
import br.com.orbe.dto.ApiResponse;
import br.com.orbe.model.Auditoria;
import br.com.orbe.model.BloqueioAgenda;
import br.com.orbe.model.ConfiguracaoAgenda;
import br.com.orbe.model.Convenio;
import br.com.orbe.model.Lote;
import br.com.orbe.service.AdministracaoService;
import br.com.orbe.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;

@WebServlet("/api/admin/*")
public class AdministracaoServlet extends BaseServlet {

    private AdministracaoService service;

    @Override
    public void init() throws ServletException {
        service = (AdministracaoService) getServletContext().getAttribute(
                ApplicationContextListener.ADMINISTRACAO_SERVICE
        );
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            Object dados = switch (recurso(request)) {
                case "usuarios" -> service.listarUsuarios();
                case "vacinas" -> service.listarVacinas();
                case "lotes" -> service.listarLotes();
                case "convenios" -> service.listarConvenios();
                case "movimentacoes" -> service.listarMovimentacoes();
                case "auditoria" -> service.listarAuditoria();
                case "horarios" -> service.listarHorarios();
                case "bloqueios" -> service.listarBloqueios();
                case "relatorio" -> service.relatorio(
                        LocalDate.parse(request.getParameter("inicio")),
                        LocalDate.parse(request.getParameter("fim"))
                );
                default -> throw new IllegalArgumentException("Recurso inválido.");
            };

            json(response, HttpServletResponse.SC_OK, ApiResponse.ok(dados));
        } catch (Exception exception) {
            handleException(response, exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        salvar(request, response, false);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        salvar(request, response, true);
    }

    private void salvar(
            HttpServletRequest request,
            HttpServletResponse response,
            boolean edicao
    ) throws IOException {
        try {
            String recurso = recurso(request);
            Object resultado = salvarRecurso(request, recurso, edicao);
            registrarAuditoria(request, recurso, resultado, edicao);

            if (edicao) {
                json(response, HttpServletResponse.SC_OK, ApiResponse.ok(resultado));
            } else {
                json(
                        response,
                        HttpServletResponse.SC_CREATED,
                        ApiResponse.criado("Registro cadastrado.", resultado)
                );
            }
        } catch (Exception exception) {
            handleException(response, exception);
        }
    }

    private Object salvarRecurso(
            HttpServletRequest request,
            String recurso,
            boolean edicao
    ) throws IOException {
        return switch (recurso) {
            case "lotes" -> salvarLote(request, edicao);
            case "convenios" -> salvarConvenio(request, edicao);
            case "horarios" -> service.salvarHorario(
                    JsonUtil.mapper().readValue(request.getReader(), ConfiguracaoAgenda.class)
            );
            case "bloqueios" -> service.salvarBloqueio(
                    JsonUtil.mapper().readValue(request.getReader(), BloqueioAgenda.class)
            );
            default -> throw new IllegalArgumentException("Operação inválida.");
        };
    }

    private Lote salvarLote(HttpServletRequest request, boolean edicao)
            throws IOException {
        Lote lote = JsonUtil.mapper().readValue(request.getReader(), Lote.class);
        if (edicao) {
            lote.setId(id(request));
        }
        return service.salvarLote(lote);
    }

    private Convenio salvarConvenio(HttpServletRequest request, boolean edicao)
            throws IOException {
        Convenio convenio = JsonUtil.mapper().readValue(request.getReader(), Convenio.class);
        if (edicao) {
            convenio.setId(id(request));
        }
        return service.salvarConvenio(convenio);
    }

    private void registrarAuditoria(
            HttpServletRequest request,
            String recurso,
            Object resultado,
            boolean edicao
    ) {
        var usuario = AutenticacaoServlet.usuarioDaSessao(request);

        Auditoria auditoria = new Auditoria();
        auditoria.setUsuarioId(usuario == null ? null : usuario.getId());
        auditoria.setAcao(edicao ? "ATUALIZAR" : "CRIAR");
        auditoria.setEntidade(recurso.toUpperCase());
        auditoria.setEntidadeId(identificadorAuditoria(resultado));
        auditoria.setDescricao(
                (edicao ? "Atualização" : "Cadastro")
                        + " realizado no módulo administrativo."
        );
        auditoria.setIp(request.getRemoteAddr());
        service.registrarAuditoria(auditoria);
    }

    private String identificadorAuditoria(Object resultado) {
        if (resultado instanceof Lote lote) {
            return String.valueOf(lote.getId());
        }
        if (resultado instanceof Convenio convenio) {
            return String.valueOf(convenio.getId());
        }
        return "CONFIGURACAO";
    }

    private String recurso(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path == null || path.length() <= 1) {
            return "";
        }
        return path.substring(1).split("/")[0];
    }

    private Long id(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path == null) {
            throw new IllegalArgumentException("Identificador obrigatório.");
        }

        String[] partes = path.substring(1).split("/");
        if (partes.length < 2) {
            throw new IllegalArgumentException("Identificador obrigatório.");
        }
        return Long.valueOf(partes[1]);
    }
}
