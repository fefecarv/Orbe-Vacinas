package br.com.orbe.config;

import br.com.orbe.exception.PersistenceException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class ConnectionFactory {

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            throw new ExceptionInInitializerError("Driver JDBC do MySQL nao encontrado.");
        }
    }

    private final DatabaseConfig config;

    public ConnectionFactory(DatabaseConfig config) {
        this.config = config;
    }

    public Connection open() {
        try {
            return DriverManager.getConnection(
                    config.url(),
                    config.usuario(),
                    config.senha()
            );
        } catch (SQLException exception) {
            throw new PersistenceException(
                    "Não foi possível conectar ao banco de dados.",
                    exception
            );
        }
    }
}
