package br.com.orbe.dao.jdbc;

import br.com.orbe.config.ConnectionFactory;
import br.com.orbe.dao.AuditoriaDao;
import br.com.orbe.model.Auditoria;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AuditoriaDaoJdbc extends AbstractJdbcDao implements AuditoriaDao {

    private static final String COLUNAS = "auditoria_id, usuario_id, acao, entidade, entidade_id, descricao, ip, criado_em";

    public AuditoriaDaoJdbc(ConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    @Override
    public Auditoria salvar(Auditoria auditoria) {
        String sql = "INSERT INTO auditoria (usuario_id,acao,entidade,entidade_id,descricao,ip) VALUES (?,?,?,?,?,?)";
        try (var connection = connectionFactory.open(); var statement = insertStatement(connection, sql)) {
            preencher(statement, auditoria);
            statement.executeUpdate();
            auditoria.setId(generatedId(statement));
            return auditoria;
        } catch (SQLException exception) {
            throw persistenceException(exception);
        }
    }

    @Override
    public Auditoria atualizar(Auditoria auditoria) {
        String sql = "UPDATE auditoria SET usuario_id=?,acao=?,entidade=?,entidade_id=?,descricao=?,ip=? WHERE auditoria_id=?";
        try (var connection = connectionFactory.open(); var statement = connection.prepareStatement(sql)) {
            preencher(statement, auditoria);
            statement.setLong(7, auditoria.getId());
            statement.executeUpdate();
            return auditoria;
        } catch (SQLException exception) {
            throw persistenceException(exception);
        }
    }

    @Override
    public Optional<Auditoria> buscarPorId(Long id) {
        return listar("SELECT " + COLUNAS + " FROM auditoria WHERE auditoria_id=?", id)
                .stream()
                .findFirst();
    }

    @Override
    public List<Auditoria> listarTodos() {
        return listar("SELECT " + COLUNAS + " FROM auditoria ORDER BY criado_em DESC");
    }

    @Override
    public List<Auditoria> listarPorUsuario(Long usuarioId) {
        return listar("SELECT " + COLUNAS + " FROM auditoria WHERE usuario_id=? ORDER BY criado_em DESC", usuarioId);
    }

    @Override
    public boolean excluir(Long id) {
        try (var connection = connectionFactory.open(); var statement = connection.prepareStatement("DELETE FROM auditoria WHERE auditoria_id=?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw persistenceException(exception);
        }
    }

    private List<Auditoria> listar(String sql, Object... parametros) {
        List<Auditoria> itens = new ArrayList<>();
        try (var connection = connectionFactory.open(); var statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) statement.setObject(i + 1, parametros[i]);
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) itens.add(mapear(resultSet));
            }
            return itens;
        } catch (SQLException exception) {
            throw persistenceException(exception);
        }
    }

    private void preencher(java.sql.PreparedStatement statement, Auditoria auditoria) throws SQLException {
        nullableLong(statement, 1, auditoria.getUsuarioId());
        statement.setString(2, auditoria.getAcao());
        statement.setString(3, auditoria.getEntidade());
        statement.setString(4, auditoria.getEntidadeId());
        statement.setString(5, auditoria.getDescricao());
        statement.setString(6, auditoria.getIp());
    }

    private Auditoria mapear(ResultSet resultSet) throws SQLException {
        Auditoria auditoria = new Auditoria();
        auditoria.setId(resultSet.getLong("auditoria_id"));
        auditoria.setUsuarioId(nullableLong(resultSet, "usuario_id"));
        auditoria.setAcao(resultSet.getString("acao"));
        auditoria.setEntidade(resultSet.getString("entidade"));
        auditoria.setEntidadeId(resultSet.getString("entidade_id"));
        auditoria.setDescricao(resultSet.getString("descricao"));
        auditoria.setIp(resultSet.getString("ip"));
        auditoria.setCriadoEm(resultSet.getTimestamp("criado_em").toLocalDateTime());
        return auditoria;
    }
}
