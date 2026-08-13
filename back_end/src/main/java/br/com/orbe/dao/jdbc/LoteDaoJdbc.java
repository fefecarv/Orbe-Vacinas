package br.com.orbe.dao.jdbc;

import br.com.orbe.config.ConnectionFactory;
import br.com.orbe.dao.LoteDao;
import br.com.orbe.model.Lote;
import br.com.orbe.model.enums.StatusLote;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LoteDaoJdbc extends AbstractJdbcDao implements LoteDao {

    private static final String COLUNAS = """
            lote_id, vacina_id, numero_lote, data_validade, quantidade_inicial,
            quantidade_atual, fornecedor, status, criado_em
            """;

    public LoteDaoJdbc(ConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    @Override
    public Lote salvar(Lote lote) {
        String sql = """
                INSERT INTO lote
                    (vacina_id, numero_lote, data_validade, quantidade_inicial,
                     quantidade_atual, fornecedor, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (var connection = connectionFactory.open();
             var statement = insertStatement(connection, sql)) {
            preencher(statement, lote);
            statement.executeUpdate();
            lote.setId(generatedId(statement));
            return lote;
        } catch (SQLException exception) {
            throw persistenceException(exception);
        }
    }

    @Override
    public Lote atualizar(Lote lote) {
        String sql = """
                UPDATE lote
                   SET vacina_id = ?, numero_lote = ?, data_validade = ?,
                       quantidade_inicial = ?, quantidade_atual = ?,
                       fornecedor = ?, status = ?
                 WHERE lote_id = ?
                """;
        try (var connection = connectionFactory.open();
             var statement = connection.prepareStatement(sql)) {
            preencher(statement, lote);
            statement.setLong(8, lote.getId());
            statement.executeUpdate();
            return lote;
        } catch (SQLException exception) {
            throw persistenceException(exception);
        }
    }

    @Override
    public Optional<Lote> buscarPorId(Long id) {
        String sql = "SELECT " + COLUNAS + " FROM lote WHERE lote_id = ?";
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
    public List<Lote> listarTodos() {
        return consultar("SELECT " + COLUNAS + " FROM lote ORDER BY data_validade");
    }

    @Override
    public List<Lote> listarValidosPorVacina(Long vacinaId) {
        String sql = """
                SELECT %s FROM lote
                 WHERE vacina_id = ?
                   AND status = 'ATIVO'
                   AND quantidade_atual > 0
                   AND data_validade >= CURRENT_DATE
                 ORDER BY data_validade
                """.formatted(COLUNAS);
        return consultar(sql, vacinaId);
    }

    @Override
    public boolean atualizarSaldo(Long loteId, int saldoEsperado, int novoSaldo) {
        String sql = """
                UPDATE lote
                   SET quantidade_atual = ?,
                       status = CASE WHEN ? = 0 THEN 'ESGOTADO' ELSE status END
                 WHERE lote_id = ? AND quantidade_atual = ?
                """;
        try (var connection = connectionFactory.open();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, novoSaldo);
            statement.setInt(2, novoSaldo);
            statement.setLong(3, loteId);
            statement.setInt(4, saldoEsperado);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw persistenceException(exception);
        }
    }

    @Override
    public boolean excluir(Long id) {
        String sql = "UPDATE lote SET status = 'BLOQUEADO' WHERE lote_id = ?";
        try (var connection = connectionFactory.open();
             var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw persistenceException(exception);
        }
    }

    private List<Lote> consultar(String sql, Object... parameters) {
        List<Lote> lotes = new ArrayList<>();
        try (var connection = connectionFactory.open();
             var statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < parameters.length; index++) {
                statement.setObject(index + 1, parameters[index]);
            }
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    lotes.add(mapear(resultSet));
                }
            }
            return lotes;
        } catch (SQLException exception) {
            throw persistenceException(exception);
        }
    }

    private void preencher(java.sql.PreparedStatement statement, Lote lote) throws SQLException {
        statement.setLong(1, lote.getVacinaId());
        statement.setString(2, lote.getNumeroLote());
        statement.setObject(3, lote.getDataValidade());
        statement.setInt(4, lote.getQuantidadeInicial());
        statement.setInt(5, lote.getQuantidadeAtual());
        statement.setString(6, lote.getFornecedor());
        statement.setString(7, lote.getStatus().name());
    }

    private Lote mapear(ResultSet resultSet) throws SQLException {
        Lote lote = new Lote();
        lote.setId(resultSet.getLong("lote_id"));
        lote.setVacinaId(resultSet.getLong("vacina_id"));
        lote.setNumeroLote(resultSet.getString("numero_lote"));
        lote.setDataValidade(resultSet.getObject("data_validade", java.time.LocalDate.class));
        lote.setQuantidadeInicial(resultSet.getInt("quantidade_inicial"));
        lote.setQuantidadeAtual(resultSet.getInt("quantidade_atual"));
        lote.setFornecedor(resultSet.getString("fornecedor"));
        lote.setStatus(StatusLote.valueOf(resultSet.getString("status")));
        lote.setCriadoEm(resultSet.getTimestamp("criado_em").toLocalDateTime());
        return lote;
    }
}
