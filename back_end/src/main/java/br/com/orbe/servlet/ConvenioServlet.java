package br.com.orbe.servlet;

import br.com.orbe.config.ApplicationContextListener;
import br.com.orbe.dto.ApiResponse;
import br.com.orbe.service.PortalPacienteService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/api/convenios/*")
public class ConvenioServlet extends BaseServlet {
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
        json(response, HttpServletResponse.SC_OK, ApiResponse.ok(
                "/aceitos".equals(request.getPathInfo())
                        ? service.listarConveniosAceitos()
                        : service.listarConvenios(usuario.getId())));
    }
    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response)throws IOException{salvar(request,response,false);}
    @Override protected void doPut(HttpServletRequest request,HttpServletResponse response)throws IOException{salvar(request,response,true);}
    private void salvar(HttpServletRequest request,HttpServletResponse response,boolean edit)throws IOException{try{var usuario=AutenticacaoServlet.usuarioDaSessao(request);var x=br.com.orbe.util.JsonUtil.mapper().readValue(request.getReader(),br.com.orbe.model.UsuarioConvenio.class);if(edit){Long id=pathId(request);if(id==null)throw new IllegalArgumentException("Informe a carteirinha.");x.setId(id);}json(response,edit?200:201,ApiResponse.ok(service.salvarConvenio(usuario.getId(),x)));}catch(Exception e){handleException(response,e);}}
}
