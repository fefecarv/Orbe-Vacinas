package br.com.orbe.servlet;

import br.com.orbe.config.ApplicationContextListener;
import br.com.orbe.dto.ApiResponse;
import br.com.orbe.dto.SalvarPacienteRequest;
import br.com.orbe.service.PacienteService;
import br.com.orbe.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/api/pacientes/*")
public class PacienteServlet extends BaseServlet {
    private PacienteService service;
    @Override public void init() throws ServletException { service=(PacienteService)getServletContext().getAttribute(ApplicationContextListener.PACIENTE_SERVICE); }
    @Override protected void doGet(HttpServletRequest request,HttpServletResponse response)throws IOException { json(response,200,ApiResponse.ok(service.listar())); }
    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response)throws IOException { try{var body=JsonUtil.mapper().readValue(request.getReader(),SalvarPacienteRequest.class);json(response,201,ApiResponse.criado("Paciente cadastrado.",service.cadastrar(body)));}catch(Exception exception){handleException(response,exception);} }
    @Override protected void doPut(HttpServletRequest request,HttpServletResponse response)throws IOException { try{String id=request.getPathInfo()==null?null:request.getPathInfo().substring(1);if(id==null||!id.matches("[UD]:\\d+"))throw new IllegalArgumentException("Paciente inválido.");var body=JsonUtil.mapper().readValue(request.getReader(),SalvarPacienteRequest.class);json(response,200,ApiResponse.ok(service.atualizar(id,body)));}catch(Exception exception){handleException(response,exception);} }
}
