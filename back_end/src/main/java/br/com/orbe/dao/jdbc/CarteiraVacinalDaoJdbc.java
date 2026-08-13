package br.com.orbe.dao.jdbc;

import br.com.orbe.config.ConnectionFactory;
import br.com.orbe.dto.CarteiraVacinalItem;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CarteiraVacinalDaoJdbc extends AbstractJdbcDao {

    private static final String SELECT_BASE = """
            SELECT a.aplicacao_id, a.protocolo, v.nome AS vacina,
                   v.fabricante, a.dose, a.data_aplicacao,
                   l.numero_lote, a.local_aplicacao,
                   profissional.nome AS profissional
              FROM aplicacao a
              JOIN lote l ON l.lote_id = a.lote_id
              JOIN vacina v ON v.vacina_id = l.vacina_id
              JOIN usuario profissional ON profissional.usuario_id = a.funcionario_id
            """;

    public CarteiraVacinalDaoJdbc(ConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    public List<CarteiraVacinalItem> listarPorUsuario(Long usuarioId) {
        return listar(SELECT_BASE
                + " WHERE a.usuario_id=? ORDER BY a.data_aplicacao DESC", usuarioId);
    }

    public List<CarteiraVacinalItem> listarPorDependente(Long dependenteId) {
        return listar(SELECT_BASE
                + " WHERE a.dependente_id=? ORDER BY a.data_aplicacao DESC", dependenteId);
    }

    private List<CarteiraVacinalItem> listar(String sql, Long pacienteId) {
        List<CarteiraVacinalItem> itens = new ArrayList<>();
        try (var connection = connectionFactory.open();
             var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, pacienteId);
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    itens.add(mapear(resultSet));
                }
            }
            return itens;
        } catch (SQLException exception) {
            throw persistenceException(exception);
        }
    }

    private CarteiraVacinalItem mapear(ResultSet resultSet) throws SQLException {
        CarteiraVacinalItem item = new CarteiraVacinalItem();
        item.setAplicacaoId(resultSet.getLong("aplicacao_id"));
        item.setProtocolo(resultSet.getString("protocolo"));
        item.setVacina(resultSet.getString("vacina"));
        item.setFabricante(resultSet.getString("fabricante"));
        item.setDose(resultSet.getString("dose"));
        item.setDataAplicacao(resultSet.getTimestamp("data_aplicacao").toLocalDateTime());
        item.setNumeroLote(resultSet.getString("numero_lote"));
        item.setLocalAplicacao(resultSet.getString("local_aplicacao"));
        item.setProfissional(resultSet.getString("profissional"));
        return item;
    }
}
