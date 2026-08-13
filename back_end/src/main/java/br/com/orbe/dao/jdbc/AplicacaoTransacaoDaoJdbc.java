package br.com.orbe.dao.jdbc;

import br.com.orbe.config.ConnectionFactory;
import br.com.orbe.dto.RegistrarAplicacaoRequest;
import br.com.orbe.exception.BusinessException;
import br.com.orbe.exception.PersistenceException;
import br.com.orbe.model.Aplicacao;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public class AplicacaoTransacaoDaoJdbc extends AbstractJdbcDao {

    public AplicacaoTransacaoDaoJdbc(ConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    public Aplicacao registrar(RegistrarAplicacaoRequest request) {
        try (Connection connection = connectionFactory.open()) {
            connection.setAutoCommit(false);
            try {
                validarFuncionario(connection, request.funcionarioId());
                LoteBloqueado lote = bloquearLote(connection, request.loteId());
                validarAgendamento(connection, request, lote.vacinaId());

                Aplicacao aplicacao = inserirAplicacao(connection, request);
                int novoSaldo = lote.quantidadeAtual() - 1;
                atualizarLote(connection, lote, novoSaldo);
                inserirMovimentacao(connection, request, aplicacao, lote.quantidadeAtual(), novoSaldo);
                concluirAgendamento(connection, request.agendamentoId());
                inserirAuditoria(connection, request, aplicacao);

                connection.commit();
                return aplicacao;
            } catch (Exception exception) {
                connection.rollback();
                if (exception instanceof BusinessException businessException) {
                    throw businessException;
                }
                if (exception instanceof SQLException sqlException) {
                    throw sqlException;
                }
                throw new SQLException("Falha inesperada durante a transacao.", exception);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new PersistenceException(
                    "Não foi possível concluir o registro da aplicação.",
                    exception
            );
        }
    }

    private void validarFuncionario(Connection connection, Long funcionarioId) throws SQLException {
        String sql = """
                SELECT 1
                  FROM usuario u
                  JOIN usuario_perfil p ON p.usuario_id = u.usuario_id
                 WHERE u.usuario_id = ?
                   AND u.status = 'ATIVO'
                   AND p.perfil IN ('FUNCIONARIO', 'ADMINISTRADOR')
                   AND p.ativo = TRUE
                """;
        try (var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, funcionarioId);
            try (var resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new BusinessException("O profissional informado não possui acesso ativo.");
                }
            }
        }
    }

    private LoteBloqueado bloquearLote(Connection connection, Long loteId) throws SQLException {
        String sql = """
                SELECT vacina_id, quantidade_atual, data_validade, status
                  FROM lote
                 WHERE lote_id = ?
                 FOR UPDATE
                """;
        try (var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, loteId);
            try (var resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new BusinessException("Lote não encontrado.");
                }
                int quantidade = resultSet.getInt("quantidade_atual");
                LocalDate validade = resultSet.getObject("data_validade", LocalDate.class);
                String status = resultSet.getString("status");
                if (!"ATIVO".equals(status) || quantidade <= 0) {
                    throw new BusinessException("O lote não possui estoque disponível.");
                }
                if (validade.isBefore(LocalDate.now())) {
                    throw new BusinessException("Não é permitido utilizar um lote vencido.");
                }
                return new LoteBloqueado(
                        loteId,
                        resultSet.getLong("vacina_id"),
                        quantidade
                );
            }
        }
    }

    private void validarAgendamento(
            Connection connection,
            RegistrarAplicacaoRequest request,
            Long vacinaIdDoLote
    ) throws SQLException {
        if (request.agendamentoId() == null) {
            return;
        }
        String sql = "SELECT vacina_id, usuario_id, dependente_id, data_agendamento, status FROM agendamento WHERE agendamento_id=? FOR UPDATE";
        try (var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, request.agendamentoId());
            try (var resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new BusinessException("Agendamento não encontrado.");
                }
                if (resultSet.getLong("vacina_id") != vacinaIdDoLote) {
                    throw new BusinessException("O lote não pertence à vacina agendada.");
                }
                String status = resultSet.getString("status");
                if (!"EM_ATENDIMENTO".equals(status)) {
                    throw new BusinessException("O agendamento deve estar em atendimento para registrar a aplicação.");
                }
                LocalDateTime dataAgendamento = resultSet.getObject(
                        "data_agendamento",
                        LocalDateTime.class
                );
                if (request.dataAplicacao().isBefore(dataAgendamento)) {
                    throw new BusinessException(
                            "A aplicação não pode ser registrada antes do horário agendado."
                    );
                }
                Long usuarioId = nullableLong(resultSet, "usuario_id");
                Long dependenteId = nullableLong(resultSet, "dependente_id");
                if (!java.util.Objects.equals(usuarioId, request.usuarioId())
                        || !java.util.Objects.equals(dependenteId, request.dependenteId())) {
                    throw new BusinessException("O paciente não corresponde ao agendamento.");
                }
            }
        }
    }

    private Aplicacao inserirAplicacao(
            Connection connection,
            RegistrarAplicacaoRequest request
    ) throws SQLException {
        String sql = """
                INSERT INTO aplicacao
                    (protocolo, agendamento_id, usuario_id, dependente_id,
                     funcionario_id, lote_id, dose, data_aplicacao,
                     tipo_atendimento, via_administracao, local_aplicacao,
                     valor_pago, observacoes)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                        COALESCE(?, (SELECT valor_estimado FROM agendamento WHERE agendamento_id = ?)), ?)
                """;
        Aplicacao aplicacao = new Aplicacao();
        aplicacao.setProtocolo(gerarProtocolo());
        aplicacao.setAgendamentoId(request.agendamentoId());
        aplicacao.setUsuarioId(request.usuarioId());
        aplicacao.setDependenteId(request.dependenteId());
        aplicacao.setFuncionarioId(request.funcionarioId());
        aplicacao.setLoteId(request.loteId());
        aplicacao.setDose(request.dose());
        aplicacao.setDataAplicacao(request.dataAplicacao());
        aplicacao.setTipoAtendimento(request.tipoAtendimento());
        aplicacao.setViaAdministracao(request.viaAdministracao());
        aplicacao.setLocalAplicacao(request.localAplicacao());
        aplicacao.setValorPago(request.valorPago());
        aplicacao.setObservacoes(request.observacoes());
        aplicacao.setCriadoEm(LocalDateTime.now());

        try (var statement = insertStatement(connection, sql)) {
            statement.setString(1, aplicacao.getProtocolo());
            nullableLong(statement, 2, aplicacao.getAgendamentoId());
            nullableLong(statement, 3, aplicacao.getUsuarioId());
            nullableLong(statement, 4, aplicacao.getDependenteId());
            statement.setLong(5, aplicacao.getFuncionarioId());
            statement.setLong(6, aplicacao.getLoteId());
            statement.setString(7, aplicacao.getDose());
            statement.setObject(8, aplicacao.getDataAplicacao());
            statement.setString(9, aplicacao.getTipoAtendimento().name());
            statement.setString(10, aplicacao.getViaAdministracao());
            statement.setString(11, aplicacao.getLocalAplicacao());
            statement.setBigDecimal(12, aplicacao.getValorPago());
            nullableLong(statement, 13, aplicacao.getAgendamentoId());
            statement.setString(14, aplicacao.getObservacoes());
            statement.executeUpdate();
            aplicacao.setId(generatedId(statement));
        }
        return aplicacao;
    }

    private void atualizarLote(
            Connection connection,
            LoteBloqueado lote,
            int novoSaldo
    ) throws SQLException {
        String sql = """
                UPDATE lote
                   SET quantidade_atual = ?,
                       status = CASE WHEN ? = 0 THEN 'ESGOTADO' ELSE status END
                 WHERE lote_id = ?
                   AND quantidade_atual = ?
                """;
        try (var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, novoSaldo);
            statement.setInt(2, novoSaldo);
            statement.setLong(3, lote.loteId());
            statement.setInt(4, lote.quantidadeAtual());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("O saldo do lote foi alterado por outra operação.");
            }
        }
    }

    private void inserirMovimentacao(
            Connection connection,
            RegistrarAplicacaoRequest request,
            Aplicacao aplicacao,
            int saldoAnterior,
            int saldoPosterior
    ) throws SQLException {
        String sql = "INSERT INTO movimentacao_estoque (lote_id,usuario_id,aplicacao_id,tipo,quantidade,saldo_anterior,saldo_posterior,motivo) VALUES (?,?,?,'APLICACAO',1,?,?,?)";
        try (var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, request.loteId());
            statement.setLong(2, request.funcionarioId());
            statement.setLong(3, aplicacao.getId());
            statement.setInt(4, saldoAnterior);
            statement.setInt(5, saldoPosterior);
            statement.setString(6, "Baixa automática por aplicação");
            statement.executeUpdate();
        }
    }

    private void concluirAgendamento(Connection connection, Long agendamentoId) throws SQLException {
        if (agendamentoId == null) return;
        try (var statement = connection.prepareStatement("UPDATE agendamento SET status='CONCLUIDO' WHERE agendamento_id=?")) {
            statement.setLong(1, agendamentoId);
            statement.executeUpdate();
        }
    }

    private void inserirAuditoria(
            Connection connection,
            RegistrarAplicacaoRequest request,
            Aplicacao aplicacao
    ) throws SQLException {
        String sql = "INSERT INTO auditoria (usuario_id,acao,entidade,entidade_id,descricao) VALUES (?,'CRIAR','APLICACAO',?,?)";
        try (var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, request.funcionarioId());
            statement.setString(2, String.valueOf(aplicacao.getId()));
            statement.setString(3, "Aplicação registrada: " + aplicacao.getProtocolo());
            statement.executeUpdate();
        }
    }

    private String gerarProtocolo() {
        return "ORB-APP-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-"
                + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    private record LoteBloqueado(Long loteId, Long vacinaId, int quantidadeAtual) {
    }
}
