package br.com.orbe.dao.jdbc;

import br.com.orbe.config.ConnectionFactory;
import br.com.orbe.exception.PersistenceException;
import br.com.orbe.model.Usuario;
import br.com.orbe.model.UsuarioPerfil;

import java.sql.Connection;
import java.sql.SQLException;

public class UsuarioCadastroDaoJdbc extends AbstractJdbcDao {

    public UsuarioCadastroDaoJdbc(ConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    public Usuario cadastrarComPerfil(
            Usuario usuario,
            UsuarioPerfil perfil
    ) {
        String usuarioSql = """
                INSERT INTO usuario
                    (nome, cpf, email, senha_hash, telefone, data_nascimento,
                     cep, logradouro, numero, complemento, bairro, cidade, estado,
                     status, verificacao_duas_etapas)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        String perfilSql = """
                INSERT INTO usuario_perfil
                    (usuario_id, perfil, matricula, cargo, ativo)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = connectionFactory.open()) {
            connection.setAutoCommit(false);
            try {
                try (var statement = insertStatement(connection, usuarioSql)) {
                    preencherUsuario(statement, usuario);
                    statement.executeUpdate();
                    usuario.setId(generatedId(statement));
                }

                perfil.setUsuarioId(usuario.getId());
                try (var statement = insertStatement(connection, perfilSql)) {
                    statement.setLong(1, perfil.getUsuarioId());
                    statement.setString(2, perfil.getPerfil().name());
                    statement.setString(3, perfil.getMatricula());
                    statement.setString(4, perfil.getCargo());
                    statement.setBoolean(5, perfil.isAtivo());
                    statement.executeUpdate();
                    perfil.setId(generatedId(statement));
                }

                connection.commit();
                return usuario;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new PersistenceException(
                    "Não foi possível cadastrar o usuário e seu perfil.",
                    exception
            );
        }
    }

    private void preencherUsuario(
            java.sql.PreparedStatement statement,
            Usuario usuario
    ) throws SQLException {
        statement.setString(1, usuario.getNome());
        statement.setString(2, usuario.getCpf());
        statement.setString(3, usuario.getEmail());
        statement.setString(4, usuario.getSenhaHash());
        statement.setString(5, usuario.getTelefone());
        statement.setObject(6, usuario.getDataNascimento());
        statement.setString(7, usuario.getCep());
        statement.setString(8, usuario.getLogradouro());
        statement.setString(9, usuario.getNumero());
        statement.setString(10, usuario.getComplemento());
        statement.setString(11, usuario.getBairro());
        statement.setString(12, usuario.getCidade());
        statement.setString(13, usuario.getEstado());
        statement.setString(14, usuario.getStatus().name());
        statement.setBoolean(15, usuario.isVerificacaoDuasEtapas());
    }
}
