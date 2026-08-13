package br.com.orbe.config;

import br.com.orbe.dao.jdbc.AgendamentoDaoJdbc;
import br.com.orbe.dao.jdbc.AuditoriaDaoJdbc;
import br.com.orbe.dao.jdbc.AgendaDiariaDaoJdbc;
import br.com.orbe.dao.jdbc.AplicacaoTransacaoDaoJdbc;
import br.com.orbe.dao.jdbc.DependenteDaoJdbc;
import br.com.orbe.dao.jdbc.CarteiraVacinalDaoJdbc;
import br.com.orbe.dao.jdbc.ConvenioDaoJdbc;
import br.com.orbe.dao.jdbc.LoteDaoJdbc;
import br.com.orbe.dao.jdbc.MovimentacaoEstoqueDaoJdbc;
import br.com.orbe.dao.jdbc.PacienteDaoJdbc;
import br.com.orbe.dao.jdbc.RecomendacaoDetalheDaoJdbc;
import br.com.orbe.dao.jdbc.RelatorioGerencialDaoJdbc;
import br.com.orbe.dao.jdbc.UsuarioCadastroDaoJdbc;
import br.com.orbe.dao.jdbc.UsuarioAdministrativoDaoJdbc;
import br.com.orbe.dao.jdbc.UsuarioDaoJdbc;
import br.com.orbe.dao.jdbc.UsuarioPerfilDaoJdbc;
import br.com.orbe.dao.jdbc.UsuarioDependenteDaoJdbc;
import br.com.orbe.dao.jdbc.UsuarioConvenioDaoJdbc;
import br.com.orbe.dao.jdbc.VacinaDaoJdbc;
import br.com.orbe.service.AgendamentoService;
import br.com.orbe.service.AdministracaoService;
import br.com.orbe.service.AdminBootstrapService;
import br.com.orbe.service.AplicacaoService;
import br.com.orbe.service.AutenticacaoService;
import br.com.orbe.service.AutorizacaoService;
import br.com.orbe.service.CatalogoService;
import br.com.orbe.service.PortalPacienteService;
import br.com.orbe.service.PacienteService;
import br.com.orbe.service.UsuarioService;
import br.com.orbe.service.impl.AgendamentoServiceImpl;
import br.com.orbe.service.impl.AdministracaoServiceImpl;
import br.com.orbe.service.impl.AdminBootstrapServiceImpl;
import br.com.orbe.service.impl.AplicacaoServiceImpl;
import br.com.orbe.service.impl.AutenticacaoServiceImpl;
import br.com.orbe.service.impl.AutorizacaoServiceImpl;
import br.com.orbe.service.impl.CatalogoServiceImpl;
import br.com.orbe.service.impl.PortalPacienteServiceImpl;
import br.com.orbe.service.impl.PacienteServiceImpl;
import br.com.orbe.service.impl.UsuarioServiceImpl;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class ApplicationContextListener implements ServletContextListener {

    public static final String CATALOGO_SERVICE = "catalogoService";
    public static final String USUARIO_SERVICE = "usuarioService";
    public static final String AGENDAMENTO_SERVICE = "agendamentoService";
    public static final String APLICACAO_SERVICE = "aplicacaoService";
    public static final String AUTENTICACAO_SERVICE = "autenticacaoService";
    public static final String AUTORIZACAO_SERVICE = "autorizacaoService";
    public static final String PORTAL_PACIENTE_SERVICE = "portalPacienteService";
    public static final String PACIENTE_SERVICE = "pacienteService";
    public static final String ADMINISTRACAO_SERVICE = "administracaoService";

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ConnectionFactory connectionFactory = new ConnectionFactory(
                DatabaseConfig.fromEnvironment()
        );
        VacinaDaoJdbc vacinaDao = new VacinaDaoJdbc(connectionFactory);
        LoteDaoJdbc loteDao = new LoteDaoJdbc(connectionFactory);
        UsuarioDaoJdbc usuarioDao = new UsuarioDaoJdbc(connectionFactory);
        UsuarioPerfilDaoJdbc usuarioPerfilDao = new UsuarioPerfilDaoJdbc(connectionFactory);
        UsuarioDependenteDaoJdbc usuarioDependenteDao = new UsuarioDependenteDaoJdbc(connectionFactory);
        DependenteDaoJdbc dependenteDao = new DependenteDaoJdbc(connectionFactory);
        ConvenioDaoJdbc convenioDao = new ConvenioDaoJdbc(connectionFactory);
        UsuarioConvenioDaoJdbc usuarioConvenioDao = new UsuarioConvenioDaoJdbc(connectionFactory);
        AgendamentoDaoJdbc agendamentoDao = new AgendamentoDaoJdbc(connectionFactory);

        CatalogoService catalogoService = new CatalogoServiceImpl(vacinaDao, loteDao);
        UsuarioService usuarioService = new UsuarioServiceImpl(
                usuarioDao,
                new UsuarioCadastroDaoJdbc(connectionFactory)
        );
        AgendamentoService agendamentoService = new AgendamentoServiceImpl(
                agendamentoDao,
                usuarioDao,
                dependenteDao,
                vacinaDao,
                new AgendaDiariaDaoJdbc(connectionFactory),
                convenioDao,
                usuarioConvenioDao
        );
        AplicacaoService aplicacaoService = new AplicacaoServiceImpl(
                new AplicacaoTransacaoDaoJdbc(connectionFactory),
                new CarteiraVacinalDaoJdbc(connectionFactory)
        );
        AutenticacaoService autenticacaoService = new AutenticacaoServiceImpl(
                usuarioDao,
                usuarioPerfilDao
        );
        AutorizacaoService autorizacaoService = new AutorizacaoServiceImpl(
                usuarioDependenteDao,
                usuarioConvenioDao
        );
        PortalPacienteService portalPacienteService = new PortalPacienteServiceImpl(
                dependenteDao,
                new RecomendacaoDetalheDaoJdbc(connectionFactory),
                convenioDao,
                usuarioConvenioDao
        );
        PacienteService pacienteService = new PacienteServiceImpl(
                new PacienteDaoJdbc(connectionFactory), usuarioService
        );
        AdministracaoService administracaoService = new AdministracaoServiceImpl(
                new UsuarioAdministrativoDaoJdbc(connectionFactory), loteDao, convenioDao,
                new MovimentacaoEstoqueDaoJdbc(connectionFactory),
                new AuditoriaDaoJdbc(connectionFactory),
                new RelatorioGerencialDaoJdbc(connectionFactory)
        );
        AdminBootstrapService adminBootstrapService = new AdminBootstrapServiceImpl(
                usuarioService,
                usuarioPerfilDao
        );

        boolean administradorCriado = adminBootstrapService.criarPrimeiroAdministrador(
                AdminBootstrapConfig.fromEnvironment()
        );
        if (administradorCriado) {
            event.getServletContext().log(
                    "Primeiro administrador criado pelas variaveis de bootstrap."
            );
        }

        event.getServletContext().setAttribute(CATALOGO_SERVICE, catalogoService);
        event.getServletContext().setAttribute(USUARIO_SERVICE, usuarioService);
        event.getServletContext().setAttribute(AGENDAMENTO_SERVICE, agendamentoService);
        event.getServletContext().setAttribute(APLICACAO_SERVICE, aplicacaoService);
        event.getServletContext().setAttribute(AUTENTICACAO_SERVICE, autenticacaoService);
        event.getServletContext().setAttribute(AUTORIZACAO_SERVICE, autorizacaoService);
        event.getServletContext().setAttribute(PORTAL_PACIENTE_SERVICE, portalPacienteService);
        event.getServletContext().setAttribute(PACIENTE_SERVICE, pacienteService);
        event.getServletContext().setAttribute(ADMINISTRACAO_SERVICE, administracaoService);
    }
}
