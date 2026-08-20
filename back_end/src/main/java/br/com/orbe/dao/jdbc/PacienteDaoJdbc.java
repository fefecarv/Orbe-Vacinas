package br.com.orbe.dao.jdbc;

import br.com.orbe.config.ConnectionFactory;
import br.com.orbe.dto.PacienteResumo;
import br.com.orbe.dto.SalvarPacienteRequest;
import br.com.orbe.exception.PersistenceException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PacienteDaoJdbc extends AbstractJdbcDao {
    public PacienteDaoJdbc(ConnectionFactory connectionFactory) { super(connectionFactory); }

    public List<PacienteResumo> listar() {
        String sql = """
                SELECT CONCAT('U:',u.usuario_id) id, 'TITULAR' tipo, u.nome, u.cpf,
                       u.data_nascimento, u.telefone, u.email, u.cep, u.logradouro, u.numero, u.complemento, u.bairro, u.cidade, u.estado, u.status, NULL responsavel_id,
                       NULL parentesco,
                       (SELECT v.nome FROM aplicacao a JOIN lote l ON l.lote_id=a.lote_id JOIN vacina v ON v.vacina_id=l.vacina_id WHERE a.usuario_id=u.usuario_id ORDER BY a.data_aplicacao DESC LIMIT 1) ultima_vacina,
                       (SELECT DATE(MAX(a.data_aplicacao)) FROM aplicacao a WHERE a.usuario_id=u.usuario_id) ultima_aplicacao
                  FROM usuario u JOIN usuario_perfil p ON p.usuario_id=u.usuario_id
                 WHERE p.perfil='PACIENTE'
                UNION ALL
                SELECT CONCAT('D:',d.dependente_id), 'DEPENDENTE', d.nome, d.cpf,
                       d.data_nascimento, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, d.status, MIN(ud.usuario_id), MIN(ud.parentesco),
                       (SELECT v.nome FROM aplicacao a JOIN lote l ON l.lote_id=a.lote_id JOIN vacina v ON v.vacina_id=l.vacina_id WHERE a.dependente_id=d.dependente_id ORDER BY a.data_aplicacao DESC LIMIT 1),
                       (SELECT DATE(MAX(a.data_aplicacao)) FROM aplicacao a WHERE a.dependente_id=d.dependente_id)
                  FROM dependente d
                  LEFT JOIN usuario_dependente ud ON ud.dependente_id=d.dependente_id
                 GROUP BY d.dependente_id,d.nome,d.cpf,d.data_nascimento,d.status
                 ORDER BY nome
                """;
        List<PacienteResumo> itens = new ArrayList<>();
        try (var connection=connectionFactory.open(); var statement=connection.prepareStatement(sql); var rs=statement.executeQuery()) {
            while(rs.next()) {
                PacienteResumo item=new PacienteResumo(); item.setId(rs.getString("id")); item.setTipo(rs.getString("tipo"));
                item.setNome(rs.getString("nome")); item.setCpf(rs.getString("cpf")); item.setDataNascimento(rs.getObject("data_nascimento",java.time.LocalDate.class));
                item.setTelefone(rs.getString("telefone")); item.setEmail(rs.getString("email")); item.setCep(rs.getString("cep")); item.setLogradouro(rs.getString("logradouro")); item.setNumero(rs.getString("numero")); item.setComplemento(rs.getString("complemento")); item.setBairro(rs.getString("bairro")); item.setCidade(rs.getString("cidade")); item.setEstado(rs.getString("estado")); item.setStatus(rs.getString("status")); item.setResponsavelId(nullableLong(rs,"responsavel_id"));
                item.setParentesco(rs.getString("parentesco")); item.setUltimaVacina(rs.getString("ultima_vacina")); item.setUltimaAplicacao(rs.getObject("ultima_aplicacao",java.time.LocalDate.class)); itens.add(item);
            }
            return itens;
        } catch(SQLException exception){throw persistenceException(exception);}
    }

    public Long cadastrarDependente(SalvarPacienteRequest request) {
        try(Connection connection=connectionFactory.open()) {
            connection.setAutoCommit(false);
            try {
                long id;
                try(var statement=insertStatement(connection,"INSERT INTO dependente (nome,cpf,data_nascimento,status) VALUES (?,?,?,?)")) {
                    statement.setString(1,request.getNome()); statement.setString(2,request.getCpf()); statement.setObject(3,request.getDataNascimento()); statement.setString(4,request.getStatus()); statement.executeUpdate(); id=generatedId(statement);
                }
                try(var statement=connection.prepareStatement("INSERT INTO usuario_dependente (usuario_id,dependente_id,parentesco,responsavel_legal,pode_agendar,pode_visualizar_carteira) VALUES (?,?,?,TRUE,TRUE,TRUE)")) {
                    statement.setLong(1,request.getResponsavelId()); statement.setLong(2,id); statement.setString(3,request.getParentesco()); statement.executeUpdate();
                }
                connection.commit(); return id;
            } catch(Exception exception) { connection.rollback(); throw exception; }
            finally { connection.setAutoCommit(true); }
        } catch(Exception exception) { throw new PersistenceException("Não foi possível cadastrar o dependente.",exception); }
    }

    public void atualizarDependente(Long id, SalvarPacienteRequest request) {
        try(var connection=connectionFactory.open()) {
            connection.setAutoCommit(false);
            try(var statement=connection.prepareStatement("UPDATE dependente SET nome=?,cpf=?,data_nascimento=?,status=? WHERE dependente_id=?")) {
                statement.setString(1,request.getNome()); statement.setString(2,request.getCpf()); statement.setObject(3,request.getDataNascimento()); statement.setString(4,request.getStatus()); statement.setLong(5,id);
                if(statement.executeUpdate()!=1)throw new SQLException("Dependente não encontrado.");
            }
            if(request.getResponsavelId()!=null&&request.getParentesco()!=null&&!request.getParentesco().isBlank()) {
                try(var statement=connection.prepareStatement("UPDATE usuario_dependente SET usuario_id=?,parentesco=? WHERE dependente_id=?")) {
                    statement.setLong(1,request.getResponsavelId());statement.setString(2,request.getParentesco());statement.setLong(3,id);statement.executeUpdate();
                }
            }
            connection.commit();
        } catch(SQLException exception){throw persistenceException(exception);}
    }
}
