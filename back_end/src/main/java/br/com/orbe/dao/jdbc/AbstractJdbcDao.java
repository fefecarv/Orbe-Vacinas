package br.com.orbe.dao.jdbc;

import br.com.orbe.config.ConnectionFactory;
import br.com.orbe.exception.PersistenceException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

public abstract class AbstractJdbcDao {

    protected final ConnectionFactory connectionFactory;

    protected AbstractJdbcDao(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    protected Long generatedId(PreparedStatement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getLong(1);
            }
        }
        throw new SQLException("O banco não retornou o identificador gerado.");
    }

    protected PreparedStatement insertStatement(
            Connection connection,
            String sql
    ) throws SQLException {
        return connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    }

    protected PersistenceException persistenceException(SQLException exception) {
        return new PersistenceException("Erro ao executar operação no banco de dados.", exception);
    }

    protected void nullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) statement.setNull(index, Types.BIGINT);
        else statement.setLong(index, value);
    }

    protected Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }
}
