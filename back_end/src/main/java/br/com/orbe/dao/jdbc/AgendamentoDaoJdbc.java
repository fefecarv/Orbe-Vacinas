package br.com.orbe.dao.jdbc;

import br.com.orbe.config.ConnectionFactory;
import br.com.orbe.dao.AgendamentoDao;
import br.com.orbe.model.Agendamento;
import br.com.orbe.model.enums.StatusAgendamento;
import br.com.orbe.model.enums.TipoAtendimento;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AgendamentoDaoJdbc extends AbstractJdbcDao implements AgendamentoDao {

    private static final String COLUNAS = "agendamento_id,protocolo,usuario_id,dependente_id,vacina_id,convenio_id,data_agendamento,unidade,sala,dose_prevista,tipo_atendimento,valor_estimado,status,motivo_cancelamento,cancelado_em,criado_em,atualizado_em";

    public AgendamentoDaoJdbc(ConnectionFactory connectionFactory) { super(connectionFactory); }

    @Override
    public Agendamento salvar(Agendamento item) {
        String sql = "INSERT INTO agendamento (protocolo,usuario_id,dependente_id,vacina_id,convenio_id,data_agendamento,unidade,sala,dose_prevista,tipo_atendimento,valor_estimado,status,motivo_cancelamento,cancelado_em) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (var connection=connectionFactory.open(); var statement=insertStatement(connection,sql)) {
            preencher(statement,item); statement.executeUpdate(); item.setId(generatedId(statement)); return item;
        } catch(SQLException exception){throw persistenceException(exception);}
    }

    @Override
    public Agendamento atualizar(Agendamento item) {
        String sql="UPDATE agendamento SET protocolo=?,usuario_id=?,dependente_id=?,vacina_id=?,convenio_id=?,data_agendamento=?,unidade=?,sala=?,dose_prevista=?,tipo_atendimento=?,valor_estimado=?,status=?,motivo_cancelamento=?,cancelado_em=? WHERE agendamento_id=?";
        try(var connection=connectionFactory.open();var statement=connection.prepareStatement(sql)){preencher(statement,item);statement.setLong(15,item.getId());statement.executeUpdate();return item;}catch(SQLException exception){throw persistenceException(exception);}
    }

    @Override public Optional<Agendamento> buscarPorId(Long id){return listar("SELECT "+COLUNAS+" FROM agendamento WHERE agendamento_id=?",id).stream().findFirst();}
    @Override public List<Agendamento> listarTodos(){return listar("SELECT "+COLUNAS+" FROM agendamento ORDER BY data_agendamento DESC");}
    @Override public List<Agendamento> listarPorUsuario(Long id){return listar("SELECT "+COLUNAS+" FROM agendamento WHERE usuario_id=? ORDER BY data_agendamento",id);}
    @Override public List<Agendamento> listarPorDependente(Long id){return listar("SELECT "+COLUNAS+" FROM agendamento WHERE dependente_id=? ORDER BY data_agendamento",id);}
    @Override public List<Agendamento> listarPorData(LocalDate data){return listar("SELECT "+COLUNAS+" FROM agendamento WHERE data_agendamento>=? AND data_agendamento<? ORDER BY data_agendamento",data.atStartOfDay(),data.plusDays(1).atStartOfDay());}
    @Override public boolean excluir(Long id){try(var c=connectionFactory.open();var ps=c.prepareStatement("UPDATE agendamento SET status='CANCELADO',cancelado_em=CURRENT_TIMESTAMP WHERE agendamento_id=?")){ps.setLong(1,id);return ps.executeUpdate()>0;}catch(SQLException e){throw persistenceException(e);}}

    private List<Agendamento> listar(String sql,Object...args){List<Agendamento> list=new ArrayList<>();try(var c=connectionFactory.open();var ps=c.prepareStatement(sql)){for(int i=0;i<args.length;i++)ps.setObject(i+1,args[i]);try(var rs=ps.executeQuery()){while(rs.next())list.add(mapear(rs));}return list;}catch(SQLException e){throw persistenceException(e);}}
    private void preencher(java.sql.PreparedStatement ps,Agendamento x)throws SQLException{ps.setString(1,x.getProtocolo());nullableLong(ps,2,x.getUsuarioId());nullableLong(ps,3,x.getDependenteId());ps.setLong(4,x.getVacinaId());nullableLong(ps,5,x.getConvenioId());ps.setObject(6,x.getDataAgendamento());ps.setString(7,x.getUnidade());ps.setString(8,x.getSala());ps.setString(9,x.getDosePrevista());ps.setString(10,x.getTipoAtendimento().name());ps.setBigDecimal(11,x.getValorEstimado());ps.setString(12,x.getStatus().name());ps.setString(13,x.getMotivoCancelamento());ps.setObject(14,x.getCanceladoEm());}
    private Agendamento mapear(ResultSet rs)throws SQLException{Agendamento x=new Agendamento();x.setId(rs.getLong("agendamento_id"));x.setProtocolo(rs.getString("protocolo"));x.setUsuarioId(nullableLong(rs,"usuario_id"));x.setDependenteId(nullableLong(rs,"dependente_id"));x.setVacinaId(rs.getLong("vacina_id"));x.setConvenioId(nullableLong(rs,"convenio_id"));x.setDataAgendamento(rs.getTimestamp("data_agendamento").toLocalDateTime());x.setUnidade(rs.getString("unidade"));x.setSala(rs.getString("sala"));x.setDosePrevista(rs.getString("dose_prevista"));x.setTipoAtendimento(TipoAtendimento.valueOf(rs.getString("tipo_atendimento")));x.setValorEstimado(rs.getBigDecimal("valor_estimado"));x.setStatus(StatusAgendamento.valueOf(rs.getString("status")));x.setMotivoCancelamento(rs.getString("motivo_cancelamento"));var cancelado=rs.getTimestamp("cancelado_em");x.setCanceladoEm(cancelado==null?null:cancelado.toLocalDateTime());x.setCriadoEm(rs.getTimestamp("criado_em").toLocalDateTime());x.setAtualizadoEm(rs.getTimestamp("atualizado_em").toLocalDateTime());return x;}
}
