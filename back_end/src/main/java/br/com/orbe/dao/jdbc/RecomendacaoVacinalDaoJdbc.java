package br.com.orbe.dao.jdbc;

import br.com.orbe.config.ConnectionFactory;
import br.com.orbe.dao.RecomendacaoVacinalDao;
import br.com.orbe.model.RecomendacaoVacinal;
import br.com.orbe.model.enums.StatusRecomendacao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RecomendacaoVacinalDaoJdbc extends AbstractJdbcDao implements RecomendacaoVacinalDao {
    private static final String COLUNAS="recomendacao_id,usuario_id,dependente_id,vacina_id,dose_recomendada,data_prevista,motivo,status,agendamento_id,criado_em,atualizado_em";
    public RecomendacaoVacinalDaoJdbc(ConnectionFactory factory){super(factory);}
    @Override public RecomendacaoVacinal salvar(RecomendacaoVacinal x){String sql="INSERT INTO recomendacao_vacinal (usuario_id,dependente_id,vacina_id,dose_recomendada,data_prevista,motivo,status,agendamento_id) VALUES (?,?,?,?,?,?,?,?)";try(var c=connectionFactory.open();var ps=insertStatement(c,sql)){preencher(ps,x);ps.executeUpdate();x.setId(generatedId(ps));return x;}catch(SQLException e){throw persistenceException(e);}}
    @Override public RecomendacaoVacinal atualizar(RecomendacaoVacinal x){String sql="UPDATE recomendacao_vacinal SET usuario_id=?,dependente_id=?,vacina_id=?,dose_recomendada=?,data_prevista=?,motivo=?,status=?,agendamento_id=? WHERE recomendacao_id=?";try(var c=connectionFactory.open();var ps=c.prepareStatement(sql)){preencher(ps,x);ps.setLong(9,x.getId());ps.executeUpdate();return x;}catch(SQLException e){throw persistenceException(e);}}
    @Override public Optional<RecomendacaoVacinal> buscarPorId(Long id){return listar("SELECT "+COLUNAS+" FROM recomendacao_vacinal WHERE recomendacao_id=?",id).stream().findFirst();}
    @Override public List<RecomendacaoVacinal> listarTodos(){return listar("SELECT "+COLUNAS+" FROM recomendacao_vacinal ORDER BY data_prevista");}
    @Override public List<RecomendacaoVacinal> listarPorUsuario(Long id){return listar("SELECT "+COLUNAS+" FROM recomendacao_vacinal WHERE usuario_id=? ORDER BY data_prevista",id);}
    @Override public List<RecomendacaoVacinal> listarPorDependente(Long id){return listar("SELECT "+COLUNAS+" FROM recomendacao_vacinal WHERE dependente_id=? ORDER BY data_prevista",id);}
    @Override public boolean excluir(Long id){try(var c=connectionFactory.open();var ps=c.prepareStatement("UPDATE recomendacao_vacinal SET status='DESCARTADA' WHERE recomendacao_id=?")){ps.setLong(1,id);return ps.executeUpdate()>0;}catch(SQLException e){throw persistenceException(e);}}
    private List<RecomendacaoVacinal> listar(String sql,Object...args){List<RecomendacaoVacinal> list=new ArrayList<>();try(var c=connectionFactory.open();var ps=c.prepareStatement(sql)){for(int i=0;i<args.length;i++)ps.setObject(i+1,args[i]);try(var rs=ps.executeQuery()){while(rs.next())list.add(mapear(rs));}return list;}catch(SQLException e){throw persistenceException(e);}}
    private void preencher(java.sql.PreparedStatement ps,RecomendacaoVacinal x)throws SQLException{nullableLong(ps,1,x.getUsuarioId());nullableLong(ps,2,x.getDependenteId());ps.setLong(3,x.getVacinaId());ps.setString(4,x.getDoseRecomendada());ps.setObject(5,x.getDataPrevista());ps.setString(6,x.getMotivo());ps.setString(7,x.getStatus().name());nullableLong(ps,8,x.getAgendamentoId());}
    private RecomendacaoVacinal mapear(ResultSet rs)throws SQLException{RecomendacaoVacinal x=new RecomendacaoVacinal();x.setId(rs.getLong("recomendacao_id"));x.setUsuarioId(nullableLong(rs,"usuario_id"));x.setDependenteId(nullableLong(rs,"dependente_id"));x.setVacinaId(rs.getLong("vacina_id"));x.setDoseRecomendada(rs.getString("dose_recomendada"));x.setDataPrevista(rs.getObject("data_prevista",java.time.LocalDate.class));x.setMotivo(rs.getString("motivo"));x.setStatus(StatusRecomendacao.valueOf(rs.getString("status")));x.setAgendamentoId(nullableLong(rs,"agendamento_id"));x.setCriadoEm(rs.getTimestamp("criado_em").toLocalDateTime());x.setAtualizadoEm(rs.getTimestamp("atualizado_em").toLocalDateTime());return x;}
}
