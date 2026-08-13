package br.com.orbe.dao.jdbc;

import br.com.orbe.config.ConnectionFactory;
import br.com.orbe.dao.UsuarioDependenteDao;
import br.com.orbe.model.UsuarioDependente;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsuarioDependenteDaoJdbc extends AbstractJdbcDao implements UsuarioDependenteDao {

    private static final String COLUNAS = "usuario_dependente_id, usuario_id, dependente_id, parentesco, responsavel_legal, pode_agendar, pode_visualizar_carteira, criado_em";

    public UsuarioDependenteDaoJdbc(ConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    @Override
    public UsuarioDependente salvar(UsuarioDependente vinculo) {
        String sql = "INSERT INTO usuario_dependente (usuario_id,dependente_id,parentesco,responsavel_legal,pode_agendar,pode_visualizar_carteira) VALUES (?,?,?,?,?,?)";
        try (var connection = connectionFactory.open(); var statement = insertStatement(connection, sql)) {
            preencher(statement, vinculo);
            statement.executeUpdate();
            vinculo.setId(generatedId(statement));
            return vinculo;
        } catch (SQLException exception) {
            throw persistenceException(exception);
        }
    }

    @Override
    public UsuarioDependente atualizar(UsuarioDependente vinculo) {
        String sql = "UPDATE usuario_dependente SET usuario_id=?,dependente_id=?,parentesco=?,responsavel_legal=?,pode_agendar=?,pode_visualizar_carteira=? WHERE usuario_dependente_id=?";
        try (var connection = connectionFactory.open(); var statement = connection.prepareStatement(sql)) {
            preencher(statement, vinculo);
            statement.setLong(7, vinculo.getId());
            statement.executeUpdate();
            return vinculo;
        } catch (SQLException exception) {
            throw persistenceException(exception);
        }
    }

    @Override
    public Optional<UsuarioDependente> buscarPorId(Long id) {
        return listar("SELECT " + COLUNAS + " FROM usuario_dependente WHERE usuario_dependente_id=?", id)
                .stream()
                .findFirst();
    }

    @Override
    public List<UsuarioDependente> listarTodos() {
        return listar("SELECT " + COLUNAS + " FROM usuario_dependente ORDER BY usuario_dependente_id");
    }

    @Override
    public List<UsuarioDependente> listarPorUsuario(Long usuarioId) {
        return listar("SELECT " + COLUNAS + " FROM usuario_dependente WHERE usuario_id=?", usuarioId);
    }

    @Override
    public boolean excluir(Long id) {
        try (var connection = connectionFactory.open(); var statement = connection.prepareStatement("DELETE FROM usuario_dependente WHERE usuario_dependente_id=?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw persistenceException(exception);
        }
    }

    private List<UsuarioDependente> listar(String sql, Object... parametros) {
        List<UsuarioDependente> itens = new ArrayList<>();
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

    private void preencher(java.sql.PreparedStatement statement, UsuarioDependente vinculo) throws SQLException {
        statement.setLong(1, vinculo.getUsuarioId());
        statement.setLong(2, vinculo.getDependenteId());
        statement.setString(3, vinculo.getParentesco());
        statement.setBoolean(4, vinculo.isResponsavelLegal());
        statement.setBoolean(5, vinculo.isPodeAgendar());
        statement.setBoolean(6, vinculo.isPodeVisualizarCarteira());
    }

    private UsuarioDependente mapear(ResultSet resultSet) throws SQLException {
        UsuarioDependente vinculo = new UsuarioDependente();
        vinculo.setId(resultSet.getLong("usuario_dependente_id"));
        vinculo.setUsuarioId(resultSet.getLong("usuario_id"));
        vinculo.setDependenteId(resultSet.getLong("dependente_id"));
        vinculo.setParentesco(resultSet.getString("parentesco"));
        vinculo.setResponsavelLegal(resultSet.getBoolean("responsavel_legal"));
        vinculo.setPodeAgendar(resultSet.getBoolean("pode_agendar"));
        vinculo.setPodeVisualizarCarteira(resultSet.getBoolean("pode_visualizar_carteira"));
        vinculo.setCriadoEm(resultSet.getTimestamp("criado_em").toLocalDateTime());
        return vinculo;
    }
}
