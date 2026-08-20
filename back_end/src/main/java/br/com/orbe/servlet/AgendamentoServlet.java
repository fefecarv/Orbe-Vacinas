package br.com.orbe.servlet;

import br.com.orbe.config.ApplicationContextListener;
import br.com.orbe.dto.ApiResponse;
import br.com.orbe.dto.CancelarAgendamentoRequest;
import br.com.orbe.dto.CriarAgendamentoRequest;
import br.com.orbe.dto.ReagendarAgendamentoRequest;
import br.com.orbe.dto.AtualizarStatusAgendamentoRequest;
import br.com.orbe.dto.UsuarioAutenticado;
import br.com.orbe.exception.ForbiddenException;
import br.com.orbe.model.Agendamento;
import br.com.orbe.model.enums.PerfilUsuario;
import br.com.orbe.service.AgendamentoService;
import br.com.orbe.service.AutorizacaoService;
import br.com.orbe.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@WebServlet("/api/agendamentos/*")
public class AgendamentoServlet extends BaseServlet {

    private AgendamentoService service;
    private AutorizacaoService autorizacaoService;

    @Override
    public void init() throws ServletException {
        service = (AgendamentoService) getServletContext().getAttribute(
                ApplicationContextListener.AGENDAMENTO_SERVICE);
        autorizacaoService = (AutorizacaoService) getServletContext().getAttribute(
                ApplicationContextListener.AUTORIZACAO_SERVICE);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            if ("horarios".equals(pathActionName(request))) {
                var data=LocalDate.parse(request.getParameter("data"));var unidade=request.getParameter("unidade");
                json(response,HttpServletResponse.SC_OK,br.com.orbe.dto.ApiResponse.ok(service.horariosDisponiveis(data,unidade)));return;
            }
            if ("analise-convenio".equals(pathActionName(request))) {
                Long vacinaId = optionalLongParameter(request, "vacinaId");
                Long convenioId = optionalLongParameter(request, "convenioId");
                if (vacinaId == null) throw new IllegalArgumentException("Informe a vacina.");
                autorizacaoService.validarConvenio(authenticated(request), convenioId);
                json(response, HttpServletResponse.SC_OK,
                        br.com.orbe.dto.ApiResponse.ok(service.analisarConvenio(vacinaId, convenioId)));
                return;
            }
            String value = request.getParameter("data");
            if (value != null && !value.isBlank()) {
                UsuarioAutenticado usuario = authenticated(request);
                requireInternal(usuario);
                String unidade = usuario.getPerfis().contains(PerfilUsuario.ADMINISTRADOR)
                        ? request.getParameter("unidade")
                        : usuario.getUnidade();
                if (unidade == null || unidade.isBlank()) unidade = "Orbe Centro";
                json(response, HttpServletResponse.SC_OK,
                        ApiResponse.ok(service.listarAgendaDetalhada(LocalDate.parse(value), unidade)));
                return;
            }
            Long usuarioId = optionalLongParameter(request, "usuarioId");
            Long dependenteId = optionalLongParameter(request, "dependenteId");
            autorizacaoService.validarAgendamento(
                    authenticated(request), usuarioId, dependenteId);
            json(response, HttpServletResponse.SC_OK,
                    ApiResponse.ok(service.listarPaciente(usuarioId, dependenteId)));
        } catch (DateTimeParseException exception) {
            error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Use a data no formato AAAA-MM-DD.");
        } catch (Exception exception) {
            handleException(response, exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            CriarAgendamentoRequest body = JsonUtil.mapper().readValue(
                    request.getReader(), CriarAgendamentoRequest.class);
            autorizacaoService.validarAgendamento(
                    authenticated(request), body.usuarioId(), body.dependenteId());
            autorizacaoService.validarConvenio(authenticated(request), body.convenioId());
            json(response, HttpServletResponse.SC_CREATED,
                    ApiResponse.criado("Agendamento criado.", service.criar(body)));
        } catch (Exception exception) {
            handleException(response, exception);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            Long id = requiredId(request);
            Agendamento agendamento = service.buscar(id);
            autorizacaoService.validarAgendamento(authenticated(request),
                    agendamento.getUsuarioId(), agendamento.getDependenteId());
            if ("status".equalsIgnoreCase(pathAction(request))) {
                requireInternal(authenticated(request));
                AtualizarStatusAgendamentoRequest body = JsonUtil.mapper().readValue(
                        request.getReader(), AtualizarStatusAgendamentoRequest.class);
                json(response, HttpServletResponse.SC_OK,
                        ApiResponse.ok(service.atualizarStatus(id, body.getStatus())));
                return;
            }
            if (!"reagendar".equalsIgnoreCase(pathAction(request))) {
                throw new IllegalArgumentException("Acao de agendamento invalida.");
            }
            ReagendarAgendamentoRequest body = JsonUtil.mapper().readValue(
                    request.getReader(), ReagendarAgendamentoRequest.class);
            json(response, HttpServletResponse.SC_OK,
                    ApiResponse.ok(service.reagendar(id, body.getNovaData())));
        } catch (Exception exception) {
            handleException(response, exception);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            Long id = requiredId(request);
            Agendamento agendamento = service.buscar(id);
            autorizacaoService.validarAgendamento(authenticated(request),
                    agendamento.getUsuarioId(), agendamento.getDependenteId());
            CancelarAgendamentoRequest body = JsonUtil.mapper().readValue(
                    request.getReader(), CancelarAgendamentoRequest.class);
            json(response, HttpServletResponse.SC_OK,
                    ApiResponse.ok(service.cancelar(id, body.getMotivo())));
        } catch (Exception exception) {
            handleException(response, exception);
        }
    }

    private UsuarioAutenticado authenticated(HttpServletRequest request) {
        return AutenticacaoServlet.usuarioDaSessao(request);
    }

    private void requireInternal(UsuarioAutenticado usuario) {
        boolean internal = usuario.getPerfis().stream().anyMatch(perfil ->
                perfil == PerfilUsuario.FUNCIONARIO
                        || perfil == PerfilUsuario.ADMINISTRADOR);
        if (!internal) {
            throw new ForbiddenException(
                    "A agenda diaria e restrita a equipe da clinica.");
        }
    }

    private Long requiredId(HttpServletRequest request) {
        Long id = pathId(request);
        if (id == null) {
            throw new IllegalArgumentException("Informe o agendamento.");
        }
        return id;
    }

    private String pathActionName(HttpServletRequest request) {
        String path = request.getPathInfo();
        return path == null ? "" : path.replace("/", "");
    }
}
