package br.com.orbe.service.impl;

import br.com.orbe.dao.jdbc.PacienteDaoJdbc;
import br.com.orbe.dto.PacienteResumo;
import br.com.orbe.dto.SalvarPacienteRequest;
import br.com.orbe.exception.BusinessException;
import br.com.orbe.model.Usuario;
import br.com.orbe.model.UsuarioPerfil;
import br.com.orbe.model.enums.PerfilUsuario;
import br.com.orbe.model.enums.StatusUsuario;
import br.com.orbe.service.PacienteService;
import br.com.orbe.service.UsuarioService;
import br.com.orbe.util.PasswordHasher;

import java.time.LocalDate;
import java.util.List;

public class PacienteServiceImpl implements PacienteService {
    private final PacienteDaoJdbc pacienteDao;
    private final UsuarioService usuarioService;
    public PacienteServiceImpl(PacienteDaoJdbc pacienteDao, UsuarioService usuarioService) { this.pacienteDao=pacienteDao; this.usuarioService=usuarioService; }
    @Override public List<PacienteResumo> listar(){return pacienteDao.listar();}
    @Override public PacienteResumo cadastrar(SalvarPacienteRequest request){
        validar(request);
        if("DEPENDENTE".equals(request.getTipo())){
            if(request.getResponsavelId()==null||request.getParentesco()==null||request.getParentesco().isBlank())throw new BusinessException("Responsável e parentesco são obrigatórios.");
            return localizar("D:"+pacienteDao.cadastrarDependente(request));
        }
        if(request.getEmail()==null||request.getEmail().isBlank())throw new BusinessException("O e-mail é obrigatório para o titular.");
        Usuario usuario=new Usuario();usuario.setNome(request.getNome());usuario.setCpf(request.getCpf());usuario.setEmail(request.getEmail());usuario.setTelefone(request.getTelefone());usuario.setDataNascimento(request.getDataNascimento());usuario.setStatus(StatusUsuario.valueOf(request.getStatus()));usuario.setSenhaHash(PasswordHasher.hash(request.getSenhaTemporaria()));
        UsuarioPerfil perfil=new UsuarioPerfil();perfil.setPerfil(PerfilUsuario.PACIENTE);perfil.setAtivo(true);
        return localizar("U:"+usuarioService.cadastrar(usuario,perfil).getId());
    }
    @Override public PacienteResumo atualizar(String id,SalvarPacienteRequest request){
        validar(request);String[] parts=id.split(":");long numericId=Long.parseLong(parts[1]);
        if("D".equals(parts[0])){pacienteDao.atualizarDependente(numericId,request);return localizar(id);}
        Usuario atual=usuarioService.buscar(numericId);atual.setNome(request.getNome());atual.setCpf(request.getCpf());atual.setTelefone(request.getTelefone());atual.setDataNascimento(request.getDataNascimento());atual.setStatus(StatusUsuario.valueOf(request.getStatus()));usuarioService.atualizar(numericId,atual);return localizar(id);
    }
    private void validar(SalvarPacienteRequest request){
        if(request==null||request.getNome()==null||request.getNome().isBlank())throw new BusinessException("O nome é obrigatório.");
        request.setCpf(request.getCpf()==null?null:request.getCpf().replaceAll("\\D",""));
        if(request.getCpf()==null||!request.getCpf().matches("\\d{11}"))throw new BusinessException("O CPF deve possuir 11 números.");
        if(request.getDataNascimento()==null||!request.getDataNascimento().isBefore(LocalDate.now()))throw new BusinessException("Informe uma data de nascimento válida.");
        if(!"TITULAR".equals(request.getTipo())&&!"DEPENDENTE".equals(request.getTipo()))throw new BusinessException("Tipo de paciente inválido.");
        if(request.getStatus()==null)request.setStatus("ATIVO");
    }
    private PacienteResumo localizar(String id){return pacienteDao.listar().stream().filter(item->id.equals(item.getId())).findFirst().orElseThrow();}
}
