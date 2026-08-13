package br.com.orbe.dao.jdbc;

import br.com.orbe.config.ConnectionFactory;
import br.com.orbe.dao.VacinaDao;
import br.com.orbe.model.Vacina;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VacinaDaoJdbc extends AbstractJdbcDao implements VacinaDao {

    private static final String COLUNAS = """
            vacina_id, nome, fabricante, descricao, categoria, indicacao,
            esquema_doses, valor_base, ativo, criado_em, atualizado_em
            """;

    public VacinaDaoJdbc(ConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    @Override
    public Vacina salvar(Vacina vacina) {
        String sql = """
                INSERT INTO vacina
                    (nome, fabricante, descricao, categoria, indicacao,
                     esquema_doses, valor_base, ativo)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (var connection = connectionFactory.open();
             var statement = insertStatement(connection, sql)) {
            preencher(statement, vacina);
            statement.executeUpdate();
            vacina.setId(generatedId(statement));
            return vacina;
        } catch (SQLException exception) {
            throw persistenceException(exception);
        }
    }

    @Override
    public Vacina atualizar(Vacina vacina) {
        String sql = """
                UPDATE vacina
                   SET nome = ?, fabricante = ?, descricao = ?, categoria = ?,
                       indicacao = ?, esquema_doses = ?, valor_base = ?, ativo = ?
                 WHERE vacina_id = ?
                """;

        try (var connection = connectionFactory.open();
             var statement = connection.prepareStatement(sql)) {
            preencher(statement, vacina);
            statement.setLong(9, vacina.getId());
            statement.executeUpdate();
            return vacina;
        } catch (SQLException exception) {
            throw persistenceException(exception);
        }
    }

    @Override
    public Optional<Vacina> buscarPorId(Long id) {
        String sql = "SELECT " + COLUNAS + " FROM vacina WHERE vacina_id = ?";

        try (var connection = connectionFactory.open();
             var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapear(resultSet)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw persistenceException(exception);
        }
    }

    @Override
    public List<Vacina> listarTodos() {
        return consultar("SELECT " + COLUNAS + " FROM vacina ORDER BY nome");
    }

    @Override
    public List<Vacina> listarDisponiveis() {
        String sql = """
                SELECT DISTINCT
                       v.vacina_id, v.nome, v.fabricante, v.descricao,
                       v.categoria, v.indicacao, v.esquema_doses,
                       v.valor_base, v.ativo, v.criado_em, v.atualizado_em
                  FROM vacina v
                 WHERE v.ativo = TRUE
                   AND EXISTS (
                       SELECT 1
                         FROM lote l
                        WHERE l.vacina_id = v.vacina_id
                          AND l.status = 'ATIVO'
                          AND l.quantidade_atual > 0
                          AND l.data_validade >= CURRENT_DATE
                   )
                 ORDER BY v.nome
                """;
        return consultar(sql);
    }

    @Override
    public boolean excluir(Long id) {
        String sql = "UPDATE vacina SET ativo = FALSE WHERE vacina_id = ?";
        try (var connection = connectionFactory.open();
             var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw persistenceException(exception);
        }
    }

    private List<Vacina> consultar(String sql) {
        List<Vacina> vacinas = new ArrayList<>();
        try (var connection = connectionFactory.open();
             var statement = connection.prepareStatement(sql);
             var resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                vacinas.add(mapear(resultSet));
            }
            return vacinas;
        } catch (SQLException exception) {
            throw persistenceException(exception);
        }
    }

    private void preencher(java.sql.PreparedStatement statement, Vacina vacina) throws SQLException {
        statement.setString(1, vacina.getNome());
        statement.setString(2, vacina.getFabricante());
        statement.setString(3, vacina.getDescricao());
        statement.setString(4, vacina.getCategoria());
        statement.setString(5, vacina.getIndicacao());
        statement.setString(6, vacina.getEsquemaDoses());
        statement.setBigDecimal(7, vacina.getValorBase());
        statement.setBoolean(8, vacina.isAtivo());
    }

    private Vacina mapear(ResultSet resultSet) throws SQLException {
        Vacina vacina = new Vacina();
        vacina.setId(resultSet.getLong("vacina_id"));
        vacina.setNome(resultSet.getString("nome"));
        vacina.setFabricante(resultSet.getString("fabricante"));
        vacina.setDescricao(resultSet.getString("descricao"));
        vacina.setCategoria(resultSet.getString("categoria"));
        vacina.setIndicacao(resultSet.getString("indicacao"));
        vacina.setEsquemaDoses(resultSet.getString("esquema_doses"));
        vacina.setValorBase(resultSet.getBigDecimal("valor_base"));
        vacina.setAtivo(resultSet.getBoolean("ativo"));
        Timestamp criado = resultSet.getTimestamp("criado_em");
        Timestamp atualizado = resultSet.getTimestamp("atualizado_em");
        vacina.setCriadoEm(criado == null ? null : criado.toLocalDateTime());
        vacina.setAtualizadoEm(atualizado == null ? null : atualizado.toLocalDateTime());
        return vacina;
    }
}
