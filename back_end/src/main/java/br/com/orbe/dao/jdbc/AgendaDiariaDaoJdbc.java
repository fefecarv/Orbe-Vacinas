package br.com.orbe.dao.jdbc;

import br.com.orbe.config.ConnectionFactory;
import br.com.orbe.dto.AgendaDiariaItem;
import br.com.orbe.model.enums.StatusAgendamento;
import br.com.orbe.model.enums.TipoAtendimento;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AgendaDiariaDaoJdbc extends AbstractJdbcDao {
    public AgendaDiariaDaoJdbc(ConnectionFactory connectionFactory) { super(connectionFactory); }

    public List<AgendaDiariaItem> listar(LocalDate data, String unidade) {
        normalizarFaltas(unidade);
        String sql = """
                SELECT a.agendamento_id, a.usuario_id, a.dependente_id,
                       COALESCE(u.nome, d.nome) AS paciente,
                       COALESCE(u.cpf, d.cpf) AS cpf,
                       v.nome AS vacina, v.vacina_id, a.dose_prevista, a.data_agendamento,
                       a.sala, a.status, a.tipo_atendimento
                  FROM agendamento a
                  LEFT JOIN usuario u ON u.usuario_id = a.usuario_id
                  LEFT JOIN dependente d ON d.dependente_id = a.dependente_id
                  JOIN vacina v ON v.vacina_id = a.vacina_id
                 WHERE a.data_agendamento >= ?
                   AND a.data_agendamento < ?
                   AND a.unidade = ?
                 ORDER BY a.data_agendamento
                """;
        List<AgendaDiariaItem> itens = new ArrayList<>();
        try (var connection = connectionFactory.open();
             var statement = connection.prepareStatement(sql)) {
            statement.setObject(1, data.atStartOfDay());
            statement.setObject(2, data.plusDays(1).atStartOfDay());
            statement.setString(3, unidade);
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    AgendaDiariaItem item = new AgendaDiariaItem();
                    item.setId(resultSet.getLong("agendamento_id"));
                    item.setUsuarioId(nullableLong(resultSet, "usuario_id"));
                    item.setDependenteId(nullableLong(resultSet, "dependente_id"));
                    item.setPaciente(resultSet.getString("paciente"));
                    item.setCpf(resultSet.getString("cpf"));
                    item.setVacina(resultSet.getString("vacina"));
                    item.setVacinaId(resultSet.getLong("vacina_id"));
                    item.setDose(resultSet.getString("dose_prevista"));
                    item.setDataAgendamento(resultSet.getTimestamp("data_agendamento").toLocalDateTime());
                    item.setSala(resultSet.getString("sala"));
                    item.setStatus(StatusAgendamento.valueOf(resultSet.getString("status")));
                    item.setTipoAtendimento(TipoAtendimento.valueOf(resultSet.getString("tipo_atendimento")));
                    itens.add(item);
                }
            }
            return itens;
        } catch (SQLException exception) { throw persistenceException(exception); }
    }

    private void normalizarFaltas(String unidade) {
        String sql = "UPDATE agendamento SET status='FALTOU' WHERE unidade=? AND status IN ('PENDENTE','CONFIRMADO') AND data_agendamento<CURRENT_TIMESTAMP";
        try (var connection=connectionFactory.open(); var statement=connection.prepareStatement(sql)) {
            statement.setString(1,unidade);statement.executeUpdate();
        } catch (SQLException exception) { throw persistenceException(exception); }
    }
}
