package br.com.orbe.dao.jdbc;

import br.com.orbe.config.ConnectionFactory;
import br.com.orbe.dao.AplicacaoDao;
import br.com.orbe.model.Aplicacao;
import br.com.orbe.model.enums.TipoAtendimento;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AplicacaoDaoJdbc extends AbstractJdbcDao implements AplicacaoDao {
    private static final String COLUNAS="aplicacao_id,protocolo,agendamento_id,usuario_id,dependente_id,funcionario_id,lote_id,dose,data_aplicacao,tipo_atendimento,via_administracao,local_aplicacao,valor_pago,observacoes,criado_em";
    public AplicacaoDaoJdbc(ConnectionFactory factory){super(factory);}
    @Override public Aplicacao salvar(Aplicacao x){String sql="INSERT INTO aplicacao (protocolo,agendamento_id,usuario_id,dependente_id,funcionario_id,lote_id,dose,data_aplicacao,tipo_atendimento,via_administracao,local_aplicacao,valor_pago,observacoes) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";try(var c=connectionFactory.open();var ps=insertStatement(c,sql)){preencher(ps,x);ps.executeUpdate();x.setId(generatedId(ps));return x;}catch(SQLException e){throw persistenceException(e);}}
    @Override public Aplicacao atualizar(Aplicacao x){String sql="UPDATE aplicacao SET protocolo=?,agendamento_id=?,usuario_id=?,dependente_id=?,funcionario_id=?,lote_id=?,dose=?,data_aplicacao=?,tipo_atendimento=?,via_administracao=?,local_aplicacao=?,valor_pago=?,observacoes=? WHERE aplicacao_id=?";try(var c=connectionFactory.open();var ps=c.prepareStatement(sql)){preencher(ps,x);ps.setLong(14,x.getId());ps.executeUpdate();return x;}catch(SQLException e){throw persistenceException(e);}}
    @Override public Optional<Aplicacao> buscarPorId(Long id){return listar("SELECT "+COLUNAS+" FROM aplicacao WHERE aplicacao_id=?",id).stream().findFirst();}
    @Override public List<Aplicacao> listarTodos(){return listar("SELECT "+COLUNAS+" FROM aplicacao ORDER BY data_aplicacao DESC");}
    @Override public List<Aplicacao> listarPorUsuario(Long id){return listar("SELECT "+COLUNAS+" FROM aplicacao WHERE usuario_id=? ORDER BY data_aplicacao DESC",id);}
    @Override public List<Aplicacao> listarPorDependente(Long id){return listar("SELECT "+COLUNAS+" FROM aplicacao WHERE dependente_id=? ORDER BY data_aplicacao DESC",id);}
    @Override public boolean excluir(Long id){try(var c=connectionFactory.open();var ps=c.prepareStatement("DELETE FROM aplicacao WHERE aplicacao_id=?")){ps.setLong(1,id);return ps.executeUpdate()>0;}catch(SQLException e){throw persistenceException(e);}}
    private List<Aplicacao> listar(String sql,Object...args){List<Aplicacao> list=new ArrayList<>();try(var c=connectionFactory.open();var ps=c.prepareStatement(sql)){for(int i=0;i<args.length;i++)ps.setObject(i+1,args[i]);try(var rs=ps.executeQuery()){while(rs.next())list.add(mapear(rs));}return list;}catch(SQLException e){throw persistenceException(e);}}
    private void preencher(java.sql.PreparedStatement ps,Aplicacao x)throws SQLException{ps.setString(1,x.getProtocolo());nullableLong(ps,2,x.getAgendamentoId());nullableLong(ps,3,x.getUsuarioId());nullableLong(ps,4,x.getDependenteId());ps.setLong(5,x.getFuncionarioId());ps.setLong(6,x.getLoteId());ps.setString(7,x.getDose());ps.setObject(8,x.getDataAplicacao());ps.setString(9,x.getTipoAtendimento().name());ps.setString(10,x.getViaAdministracao());ps.setString(11,x.getLocalAplicacao());ps.setBigDecimal(12,x.getValorPago());ps.setString(13,x.getObservacoes());}
    private Aplicacao mapear(ResultSet rs)throws SQLException{Aplicacao x=new Aplicacao();x.setId(rs.getLong("aplicacao_id"));x.setProtocolo(rs.getString("protocolo"));x.setAgendamentoId(nullableLong(rs,"agendamento_id"));x.setUsuarioId(nullableLong(rs,"usuario_id"));x.setDependenteId(nullableLong(rs,"dependente_id"));x.setFuncionarioId(rs.getLong("funcionario_id"));x.setLoteId(rs.getLong("lote_id"));x.setDose(rs.getString("dose"));x.setDataAplicacao(rs.getTimestamp("data_aplicacao").toLocalDateTime());x.setTipoAtendimento(TipoAtendimento.valueOf(rs.getString("tipo_atendimento")));x.setViaAdministracao(rs.getString("via_administracao"));x.setLocalAplicacao(rs.getString("local_aplicacao"));x.setValorPago(rs.getBigDecimal("valor_pago"));x.setObservacoes(rs.getString("observacoes"));x.setCriadoEm(rs.getTimestamp("criado_em").toLocalDateTime());return x;}
}
