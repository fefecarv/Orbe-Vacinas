package br.com.orbe.dao.jdbc;

import br.com.orbe.config.ConnectionFactory;
import br.com.orbe.exception.PersistenceException;
import br.com.orbe.model.UsuarioConvenio;
import java.sql.SQLException;
import java.util.*;

public class UsuarioConvenioDaoJdbc extends AbstractJdbcDao {
    public UsuarioConvenioDaoJdbc(ConnectionFactory f){super(f);}
    public List<UsuarioConvenio> listarPorUsuario(Long usuarioId){
        String sql="SELECT uc.usuario_convenio_id,uc.usuario_id,uc.convenio_id,uc.numero_carteirinha,uc.titular,uc.data_validade,uc.criado_em,c.nome,c.plano FROM usuario_convenio uc JOIN convenio c ON c.convenio_id=uc.convenio_id WHERE uc.usuario_id=? ORDER BY c.nome,c.plano";
        List<UsuarioConvenio> list=new ArrayList<>();try(var c=connectionFactory.open();var ps=c.prepareStatement(sql)){ps.setLong(1,usuarioId);try(var rs=ps.executeQuery()){while(rs.next()){UsuarioConvenio x=new UsuarioConvenio();x.setId(rs.getLong(1));x.setUsuarioId(rs.getLong(2));x.setConvenioId(rs.getLong(3));x.setNumeroCarteirinha(rs.getString(4));x.setTitular(rs.getString(5));x.setDataValidade(rs.getObject(6,java.time.LocalDate.class));x.setCriadoEm(rs.getTimestamp(7).toLocalDateTime());x.setNomeConvenio(rs.getString(8));x.setPlano(rs.getString(9));list.add(x);}}return list;}catch(SQLException e){throw persistenceException(e);}
    }
    public Optional<UsuarioConvenio> buscar(Long id){return listar("SELECT usuario_convenio_id,usuario_id,convenio_id,numero_carteirinha,titular,data_validade,criado_em FROM usuario_convenio WHERE usuario_convenio_id=?",id);}
    private Optional<UsuarioConvenio> listar(String sql,Long id){try(var c=connectionFactory.open();var ps=c.prepareStatement(sql)){ps.setLong(1,id);try(var rs=ps.executeQuery()){if(!rs.next())return Optional.empty();UsuarioConvenio x=new UsuarioConvenio();x.setId(rs.getLong(1));x.setUsuarioId(rs.getLong(2));x.setConvenioId(rs.getLong(3));x.setNumeroCarteirinha(rs.getString(4));x.setTitular(rs.getString(5));x.setDataValidade(rs.getObject(6,java.time.LocalDate.class));x.setCriadoEm(rs.getTimestamp(7).toLocalDateTime());return Optional.of(x);}}catch(SQLException e){throw persistenceException(e);}}
    public UsuarioConvenio salvar(UsuarioConvenio x){String sql=x.getId()==null?"INSERT INTO usuario_convenio(usuario_id,convenio_id,numero_carteirinha,titular,data_validade) VALUES(?,?,?,?,?)":"UPDATE usuario_convenio SET convenio_id=?,numero_carteirinha=?,titular=?,data_validade=? WHERE usuario_convenio_id=? AND usuario_id=?";try(var c=connectionFactory.open();var ps=x.getId()==null?insertStatement(c,sql):c.prepareStatement(sql)){if(x.getId()==null){ps.setLong(1,x.getUsuarioId());ps.setLong(2,x.getConvenioId());ps.setString(3,x.getNumeroCarteirinha());ps.setString(4,x.getTitular());ps.setObject(5,x.getDataValidade());ps.executeUpdate();x.setId(generatedId(ps));}else{ps.setLong(1,x.getConvenioId());ps.setString(2,x.getNumeroCarteirinha());ps.setString(3,x.getTitular());ps.setObject(4,x.getDataValidade());ps.setLong(5,x.getId());ps.setLong(6,x.getUsuarioId());ps.executeUpdate();}return x;}catch(SQLException e){throw persistenceException(e);}}
}
