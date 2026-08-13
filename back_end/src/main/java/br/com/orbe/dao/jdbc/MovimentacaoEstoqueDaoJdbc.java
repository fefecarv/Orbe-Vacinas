package br.com.orbe.dao.jdbc;

import br.com.orbe.config.ConnectionFactory;
import br.com.orbe.dao.MovimentacaoEstoqueDao;
import br.com.orbe.model.MovimentacaoEstoque;
import br.com.orbe.model.enums.TipoMovimentacao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MovimentacaoEstoqueDaoJdbc extends AbstractJdbcDao implements MovimentacaoEstoqueDao {
    private static final String COLUNAS="movimentacao_estoque_id,lote_id,usuario_id,aplicacao_id,tipo,quantidade,saldo_anterior,saldo_posterior,motivo,criado_em";
    public MovimentacaoEstoqueDaoJdbc(ConnectionFactory factory){super(factory);}
    @Override public MovimentacaoEstoque salvar(MovimentacaoEstoque x){String sql="INSERT INTO movimentacao_estoque (lote_id,usuario_id,aplicacao_id,tipo,quantidade,saldo_anterior,saldo_posterior,motivo) VALUES (?,?,?,?,?,?,?,?)";try(var c=connectionFactory.open();var ps=insertStatement(c,sql)){preencher(ps,x);ps.executeUpdate();x.setId(generatedId(ps));return x;}catch(SQLException e){throw persistenceException(e);}}
    @Override public MovimentacaoEstoque atualizar(MovimentacaoEstoque x){String sql="UPDATE movimentacao_estoque SET lote_id=?,usuario_id=?,aplicacao_id=?,tipo=?,quantidade=?,saldo_anterior=?,saldo_posterior=?,motivo=? WHERE movimentacao_estoque_id=?";try(var c=connectionFactory.open();var ps=c.prepareStatement(sql)){preencher(ps,x);ps.setLong(9,x.getId());ps.executeUpdate();return x;}catch(SQLException e){throw persistenceException(e);}}
    @Override public Optional<MovimentacaoEstoque> buscarPorId(Long id){return listar("SELECT "+COLUNAS+" FROM movimentacao_estoque WHERE movimentacao_estoque_id=?",id).stream().findFirst();}
    @Override public List<MovimentacaoEstoque> listarTodos(){return listar("SELECT "+COLUNAS+" FROM movimentacao_estoque ORDER BY criado_em DESC");}
    @Override public List<MovimentacaoEstoque> listarPorLote(Long id){return listar("SELECT "+COLUNAS+" FROM movimentacao_estoque WHERE lote_id=? ORDER BY criado_em DESC",id);}
    @Override public boolean excluir(Long id){try(var c=connectionFactory.open();var ps=c.prepareStatement("DELETE FROM movimentacao_estoque WHERE movimentacao_estoque_id=?")){ps.setLong(1,id);return ps.executeUpdate()>0;}catch(SQLException e){throw persistenceException(e);}}
    private List<MovimentacaoEstoque> listar(String sql,Object...args){List<MovimentacaoEstoque> list=new ArrayList<>();try(var c=connectionFactory.open();var ps=c.prepareStatement(sql)){for(int i=0;i<args.length;i++)ps.setObject(i+1,args[i]);try(var rs=ps.executeQuery()){while(rs.next())list.add(mapear(rs));}return list;}catch(SQLException e){throw persistenceException(e);}}
    private void preencher(java.sql.PreparedStatement ps,MovimentacaoEstoque x)throws SQLException{ps.setLong(1,x.getLoteId());ps.setLong(2,x.getUsuarioId());nullableLong(ps,3,x.getAplicacaoId());ps.setString(4,x.getTipo().name());ps.setInt(5,x.getQuantidade());ps.setInt(6,x.getSaldoAnterior());ps.setInt(7,x.getSaldoPosterior());ps.setString(8,x.getMotivo());}
    private MovimentacaoEstoque mapear(ResultSet rs)throws SQLException{MovimentacaoEstoque x=new MovimentacaoEstoque();x.setId(rs.getLong("movimentacao_estoque_id"));x.setLoteId(rs.getLong("lote_id"));x.setUsuarioId(rs.getLong("usuario_id"));x.setAplicacaoId(nullableLong(rs,"aplicacao_id"));x.setTipo(TipoMovimentacao.valueOf(rs.getString("tipo")));x.setQuantidade(rs.getInt("quantidade"));x.setSaldoAnterior(rs.getInt("saldo_anterior"));x.setSaldoPosterior(rs.getInt("saldo_posterior"));x.setMotivo(rs.getString("motivo"));x.setCriadoEm(rs.getTimestamp("criado_em").toLocalDateTime());return x;}
}
