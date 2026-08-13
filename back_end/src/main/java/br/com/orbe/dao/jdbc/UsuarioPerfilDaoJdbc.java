package br.com.orbe.dao.jdbc;

import br.com.orbe.config.ConnectionFactory;
import br.com.orbe.dao.UsuarioPerfilDao;
import br.com.orbe.model.UsuarioPerfil;
import br.com.orbe.model.enums.PerfilUsuario;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsuarioPerfilDaoJdbc extends AbstractJdbcDao implements UsuarioPerfilDao {

    private static final String COLUNAS = "usuario_perfil_id, usuario_id, perfil, matricula, cargo, ativo, criado_em";

    public UsuarioPerfilDaoJdbc(ConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    @Override
    public UsuarioPerfil salvar(UsuarioPerfil perfil) {
        String sql = "INSERT INTO usuario_perfil (usuario_id, perfil, matricula, cargo, ativo) VALUES (?, ?, ?, ?, ?)";
        try (var connection = connectionFactory.open(); var statement = insertStatement(connection, sql)) {
            preencher(statement, perfil);
            statement.executeUpdate();
            perfil.setId(generatedId(statement));
            return perfil;
        } catch (SQLException exception) {
            throw persistenceException(exception);
        }
    }

    @Override
    public UsuarioPerfil atualizar(UsuarioPerfil perfil) {
        String sql = "UPDATE usuario_perfil SET usuario_id=?, perfil=?, matricula=?, cargo=?, ativo=? WHERE usuario_perfil_id=?";
        try (var connection = connectionFactory.open(); var statement = connection.prepareStatement(sql)) {
            preencher(statement, perfil);
            statement.setLong(6, perfil.getId());
            statement.executeUpdate();
            return perfil;
        } catch (SQLException exception) {
            throw persistenceException(exception);
        }
    }

    @Override public Optional<UsuarioPerfil> buscarPorId(Long id) { return buscarUm("usuario_perfil_id", id); }
    @Override public List<UsuarioPerfil> listarTodos() { return listar("SELECT " + COLUNAS + " FROM usuario_perfil ORDER BY usuario_perfil_id"); }
    @Override public List<UsuarioPerfil> listarPorUsuario(Long usuarioId) { return listar("SELECT " + COLUNAS + " FROM usuario_perfil WHERE usuario_id=?", usuarioId); }

    @Override
    public boolean existeAdministrador() {
        String sql = "SELECT 1 FROM usuario_perfil WHERE perfil='ADMINISTRADOR' LIMIT 1";
        try (var connection = connectionFactory.open();
             var statement = connection.prepareStatement(sql);
             var resultSet = statement.executeQuery()) {
            return resultSet.next();
        } catch (SQLException exception) {
            throw persistenceException(exception);
        }
    }

    @Override
    public boolean excluir(Long id) {
        return executar("UPDATE usuario_perfil SET ativo=FALSE WHERE usuario_perfil_id=?", id);
    }

    private Optional<UsuarioPerfil> buscarUm(String coluna, Object valor) {
        List<UsuarioPerfil> itens = listar("SELECT " + COLUNAS + " FROM usuario_perfil WHERE " + coluna + "=?", valor);
        return itens.stream().findFirst();
    }

    private List<UsuarioPerfil> listar(String sql, Object... parametros) {
        List<UsuarioPerfil> itens = new ArrayList<>();
        try (var connection = connectionFactory.open(); var statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) statement.setObject(i + 1, parametros[i]);
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) itens.add(mapear(resultSet));
            }
            return itens;
        } catch (SQLException exception) { throw persistenceException(exception); }
    }

    private boolean executar(String sql, Long id) {
        try (var connection = connectionFactory.open(); var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) { throw persistenceException(exception); }
    }

    private void preencher(java.sql.PreparedStatement statement, UsuarioPerfil perfil) throws SQLException {
        statement.setLong(1, perfil.getUsuarioId());
        statement.setString(2, perfil.getPerfil().name());
        statement.setString(3, perfil.getMatricula());
        statement.setString(4, perfil.getCargo());
        statement.setBoolean(5, perfil.isAtivo());
    }

    private UsuarioPerfil mapear(ResultSet resultSet) throws SQLException {
        UsuarioPerfil perfil = new UsuarioPerfil();
        perfil.setId(resultSet.getLong("usuario_perfil_id"));
        perfil.setUsuarioId(resultSet.getLong("usuario_id"));
        perfil.setPerfil(PerfilUsuario.valueOf(resultSet.getString("perfil")));
        perfil.setMatricula(resultSet.getString("matricula"));
        perfil.setCargo(resultSet.getString("cargo"));
        perfil.setAtivo(resultSet.getBoolean("ativo"));
        perfil.setCriadoEm(resultSet.getTimestamp("criado_em").toLocalDateTime());
        return perfil;
    }
}
