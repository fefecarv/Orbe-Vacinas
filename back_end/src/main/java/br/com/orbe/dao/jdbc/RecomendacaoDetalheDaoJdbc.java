package br.com.orbe.dao.jdbc;

import br.com.orbe.config.ConnectionFactory;
import br.com.orbe.dto.RecomendacaoVacinalItem;
import br.com.orbe.model.enums.StatusRecomendacao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RecomendacaoDetalheDaoJdbc extends AbstractJdbcDao {
    private static final String SELECT = """
            SELECT r.recomendacao_id, v.nome AS vacina, r.dose_recomendada,
                   r.data_prevista, r.motivo, r.status, r.agendamento_id
              FROM recomendacao_vacinal r
              JOIN vacina v ON v.vacina_id = r.vacina_id
            """;

    public RecomendacaoDetalheDaoJdbc(ConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    public List<RecomendacaoVacinalItem> listarPorUsuario(Long id) {
        return listar(SELECT + " WHERE r.usuario_id=? AND r.status NOT IN ('CONCLUIDA','DESCARTADA') ORDER BY r.data_prevista", id);
    }

    public List<RecomendacaoVacinalItem> listarPorDependente(Long id) {
        return listar(SELECT + " WHERE r.dependente_id=? AND r.status NOT IN ('CONCLUIDA','DESCARTADA') ORDER BY r.data_prevista", id);
    }

    private List<RecomendacaoVacinalItem> listar(String sql, Long id) {
        List<RecomendacaoVacinalItem> itens = new ArrayList<>();
        try (var connection = connectionFactory.open();
             var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    RecomendacaoVacinalItem item = new RecomendacaoVacinalItem();
                    item.setId(resultSet.getLong("recomendacao_id"));
                    item.setVacina(resultSet.getString("vacina"));
                    item.setDose(resultSet.getString("dose_recomendada"));
                    item.setDataPrevista(resultSet.getObject("data_prevista", java.time.LocalDate.class));
                    item.setMotivo(resultSet.getString("motivo"));
                    item.setStatus(StatusRecomendacao.valueOf(resultSet.getString("status")));
                    item.setAgendamentoId(nullableLong(resultSet, "agendamento_id"));
                    itens.add(item);
                }
            }
            return itens;
        } catch (SQLException exception) {
            throw persistenceException(exception);
        }
    }
}
