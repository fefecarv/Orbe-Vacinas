package br.com.orbe.dao.jdbc;

import br.com.orbe.config.ConnectionFactory;
import br.com.orbe.dao.ConvenioDao;
import br.com.orbe.model.Convenio;
import br.com.orbe.model.enums.TipoCobertura;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConvenioDaoJdbc extends AbstractJdbcDao implements ConvenioDao {

    private static final String COLUNAS = "convenio_id,nome,plano,codigo_operacional,ativo,tipo_cobertura,percentual_desconto,valor_coparticipacao,criado_em,atualizado_em";

    public ConvenioDaoJdbc(ConnectionFactory connectionFactory) { super(connectionFactory); }

    @Override
    public Convenio salvar(Convenio convenio) {
        String sql = "INSERT INTO convenio (nome,plano,codigo_operacional,ativo,tipo_cobertura,percentual_desconto,valor_coparticipacao) VALUES (?,?,?,?,?,?,?)";
        try (var c = connectionFactory.open(); var ps = insertStatement(c, sql)) {
            preencher(ps, convenio); ps.executeUpdate(); convenio.setId(generatedId(ps)); return convenio;
        } catch (SQLException e) { throw persistenceException(e); }
    }

    @Override
    public Convenio atualizar(Convenio convenio) {
        String sql = "UPDATE convenio SET nome=?,plano=?,codigo_operacional=?,ativo=?,tipo_cobertura=?,percentual_desconto=?,valor_coparticipacao=? WHERE convenio_id=?";
        try (var c = connectionFactory.open(); var ps = c.prepareStatement(sql)) {
            preencher(ps, convenio); ps.setLong(8, convenio.getId()); ps.executeUpdate(); return convenio;
        } catch (SQLException e) { throw persistenceException(e); }
    }

    @Override public Optional<Convenio> buscarPorId(Long id) { return listar("SELECT "+COLUNAS+" FROM convenio WHERE convenio_id=?",id).stream().findFirst(); }
    @Override public List<Convenio> listarTodos() { return listar("SELECT "+COLUNAS+" FROM convenio ORDER BY nome,plano"); }
    @Override public boolean excluir(Long id) { try(var c=connectionFactory.open();var ps=c.prepareStatement("UPDATE convenio SET ativo=FALSE WHERE convenio_id=?")){ps.setLong(1,id);return ps.executeUpdate()>0;}catch(SQLException e){throw persistenceException(e);} }

    private List<Convenio> listar(String sql,Object...args){List<Convenio> list=new ArrayList<>();try(var c=connectionFactory.open();var ps=c.prepareStatement(sql)){for(int i=0;i<args.length;i++)ps.setObject(i+1,args[i]);try(var rs=ps.executeQuery()){while(rs.next())list.add(mapear(rs));}return list;}catch(SQLException e){throw persistenceException(e);}}
    private void preencher(java.sql.PreparedStatement ps,Convenio x)throws SQLException{ps.setString(1,x.getNome());ps.setString(2,x.getPlano());ps.setString(3,x.getCodigoOperacional());ps.setBoolean(4,x.isAtivo());ps.setString(5,(x.getTipoCobertura()==null?TipoCobertura.ANALISE_MANUAL:x.getTipoCobertura()).name());ps.setBigDecimal(6,x.getPercentualDesconto());ps.setBigDecimal(7,x.getValorCoparticipacao());}
    private Convenio mapear(ResultSet rs)throws SQLException{Convenio x=new Convenio();x.setId(rs.getLong("convenio_id"));x.setNome(rs.getString("nome"));x.setPlano(rs.getString("plano"));x.setCodigoOperacional(rs.getString("codigo_operacional"));x.setAtivo(rs.getBoolean("ativo"));x.setTipoCobertura(TipoCobertura.valueOf(rs.getString("tipo_cobertura")));x.setPercentualDesconto(rs.getBigDecimal("percentual_desconto"));x.setValorCoparticipacao(rs.getBigDecimal("valor_coparticipacao"));x.setCriadoEm(rs.getTimestamp("criado_em").toLocalDateTime());x.setAtualizadoEm(rs.getTimestamp("atualizado_em").toLocalDateTime());return x;}
}
