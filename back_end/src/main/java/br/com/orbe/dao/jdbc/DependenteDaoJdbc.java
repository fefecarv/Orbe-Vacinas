package br.com.orbe.dao.jdbc;

import br.com.orbe.config.ConnectionFactory;
import br.com.orbe.dao.DependenteDao;
import br.com.orbe.model.Dependente;
import br.com.orbe.model.enums.StatusCadastro;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DependenteDaoJdbc extends AbstractJdbcDao implements DependenteDao {

    private static final String COLUNAS = "d.dependente_id, d.nome, d.cpf, d.data_nascimento, d.sexo, d.observacoes, d.status, d.criado_em, d.atualizado_em";

    public DependenteDaoJdbc(ConnectionFactory connectionFactory) { super(connectionFactory); }

    @Override
    public Dependente salvar(Dependente d) {
        String sql = "INSERT INTO dependente (nome,cpf,data_nascimento,sexo,observacoes,status) VALUES (?,?,?,?,?,?)";
        try (var c=connectionFactory.open(); var ps=insertStatement(c,sql)) {
            preencher(ps,d); ps.executeUpdate(); d.setId(generatedId(ps)); return d;
        } catch(SQLException e){throw persistenceException(e);}
    }

    @Override
    public Dependente atualizar(Dependente d) {
        String sql="UPDATE dependente SET nome=?,cpf=?,data_nascimento=?,sexo=?,observacoes=?,status=? WHERE dependente_id=?";
        try(var c=connectionFactory.open();var ps=c.prepareStatement(sql)){preencher(ps,d);ps.setLong(7,d.getId());ps.executeUpdate();return d;}catch(SQLException e){throw persistenceException(e);}
    }

    @Override public Optional<Dependente> buscarPorId(Long id){return listar("SELECT "+COLUNAS+" FROM dependente d WHERE d.dependente_id=?",id).stream().findFirst();}
    @Override public List<Dependente> listarTodos(){return listar("SELECT "+COLUNAS+" FROM dependente d ORDER BY d.nome");}
    @Override public List<Dependente> listarPorResponsavel(Long usuarioId){return listar("SELECT "+COLUNAS+" FROM dependente d JOIN usuario_dependente ud ON ud.dependente_id=d.dependente_id WHERE ud.usuario_id=? ORDER BY d.nome",usuarioId);}
    @Override public boolean excluir(Long id){try(var c=connectionFactory.open();var ps=c.prepareStatement("UPDATE dependente SET status='INATIVO' WHERE dependente_id=?")){ps.setLong(1,id);return ps.executeUpdate()>0;}catch(SQLException e){throw persistenceException(e);}}

    private List<Dependente> listar(String sql,Object...args){List<Dependente> list=new ArrayList<>();try(var c=connectionFactory.open();var ps=c.prepareStatement(sql)){for(int i=0;i<args.length;i++)ps.setObject(i+1,args[i]);try(var rs=ps.executeQuery()){while(rs.next())list.add(mapear(rs));}return list;}catch(SQLException e){throw persistenceException(e);}}
    private void preencher(java.sql.PreparedStatement ps,Dependente d)throws SQLException{ps.setString(1,d.getNome());ps.setString(2,d.getCpf());ps.setObject(3,d.getDataNascimento());ps.setString(4,d.getSexo());ps.setString(5,d.getObservacoes());ps.setString(6,d.getStatus().name());}
    private Dependente mapear(ResultSet rs)throws SQLException{Dependente d=new Dependente();d.setId(rs.getLong("dependente_id"));d.setNome(rs.getString("nome"));d.setCpf(rs.getString("cpf"));d.setDataNascimento(rs.getObject("data_nascimento",java.time.LocalDate.class));d.setSexo(rs.getString("sexo"));d.setObservacoes(rs.getString("observacoes"));d.setStatus(StatusCadastro.valueOf(rs.getString("status")));d.setCriadoEm(rs.getTimestamp("criado_em").toLocalDateTime());d.setAtualizadoEm(rs.getTimestamp("atualizado_em").toLocalDateTime());return d;}
}
