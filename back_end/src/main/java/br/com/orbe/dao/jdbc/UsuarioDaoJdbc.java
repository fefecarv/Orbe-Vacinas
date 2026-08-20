package br.com.orbe.dao.jdbc;

import br.com.orbe.config.ConnectionFactory;
import br.com.orbe.dao.UsuarioDao;
import br.com.orbe.model.Usuario;
import br.com.orbe.model.enums.StatusUsuario;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsuarioDaoJdbc extends AbstractJdbcDao implements UsuarioDao {

    private static final String COLUNAS = "usuario_id,nome,cpf,email,senha_hash,telefone,data_nascimento,cep,logradouro,numero,complemento,bairro,cidade,estado,status,verificacao_duas_etapas,troca_senha_obrigatoria,unidade,ultimo_acesso_em,ultimo_ip,token_recuperacao_hash,token_recuperacao_expira_em,criado_em,atualizado_em";

    public UsuarioDaoJdbc(ConnectionFactory connectionFactory) { super(connectionFactory); }

    @Override
    public Usuario salvar(Usuario u) {
        String sql = "INSERT INTO usuario (nome,cpf,email,senha_hash,telefone,data_nascimento,cep,logradouro,numero,complemento,bairro,cidade,estado,status,verificacao_duas_etapas,troca_senha_obrigatoria,unidade) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (var c = connectionFactory.open(); var ps = insertStatement(c, sql)) {
            preencher(ps, u); ps.executeUpdate(); u.setId(generatedId(ps)); return u;
        } catch (SQLException e) { throw persistenceException(e); }
    }

    @Override
    public Usuario atualizar(Usuario u) {
        String sql = "UPDATE usuario SET nome=?,cpf=?,email=?,senha_hash=?,telefone=?,data_nascimento=?,cep=?,logradouro=?,numero=?,complemento=?,bairro=?,cidade=?,estado=?,status=?,verificacao_duas_etapas=?,troca_senha_obrigatoria=?,unidade=? WHERE usuario_id=?";
        try (var c = connectionFactory.open(); var ps = c.prepareStatement(sql)) {
            preencher(ps, u); ps.setLong(18, u.getId()); ps.executeUpdate(); return u;
        } catch (SQLException e) { throw persistenceException(e); }
    }

    @Override public Optional<Usuario> buscarPorId(Long id) { return buscar("usuario_id", id); }
    @Override public Optional<Usuario> buscarPorEmail(String email) { return buscar("email", email); }
    @Override public Optional<Usuario> buscarPorCpf(String cpf) { return buscar("cpf", cpf); }

    @Override
    public void registrarAcesso(Long usuarioId, String enderecoIp) {
        String sql = "UPDATE usuario SET ultimo_acesso_em=CURRENT_TIMESTAMP, ultimo_ip=? WHERE usuario_id=?";
        try (var connection = connectionFactory.open();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, enderecoIp);
            statement.setLong(2, usuarioId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw persistenceException(exception);
        }
    }

    @Override
    public List<Usuario> listarTodos() {
        String sql = "SELECT " + COLUNAS + " FROM usuario ORDER BY nome";
        List<Usuario> result = new ArrayList<>();
        try (var c = connectionFactory.open(); var ps = c.prepareStatement(sql); var rs = ps.executeQuery()) {
            while (rs.next()) result.add(mapear(rs)); return result;
        } catch (SQLException e) { throw persistenceException(e); }
    }

    @Override
    public boolean excluir(Long id) {
        try (var c = connectionFactory.open(); var ps = c.prepareStatement("UPDATE usuario SET status='INATIVO' WHERE usuario_id=?")) {
            ps.setLong(1, id); return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw persistenceException(e); }
    }

    private Optional<Usuario> buscar(String coluna, Object valor) {
        String sql = "SELECT " + COLUNAS + " FROM usuario WHERE " + coluna + "=?";
        try (var c = connectionFactory.open(); var ps = c.prepareStatement(sql)) {
            ps.setObject(1, valor); try (var rs = ps.executeQuery()) { return rs.next() ? Optional.of(mapear(rs)) : Optional.empty(); }
        } catch (SQLException e) { throw persistenceException(e); }
    }

    private void preencher(java.sql.PreparedStatement ps, Usuario u) throws SQLException {
        ps.setString(1,u.getNome()); ps.setString(2,u.getCpf()); ps.setString(3,u.getEmail()); ps.setString(4,u.getSenhaHash());
        ps.setString(5,u.getTelefone()); ps.setObject(6,u.getDataNascimento()); ps.setString(7,u.getCep()); ps.setString(8,u.getLogradouro());
        ps.setString(9,u.getNumero()); ps.setString(10,u.getComplemento()); ps.setString(11,u.getBairro()); ps.setString(12,u.getCidade());
        ps.setString(13,u.getEstado()); ps.setString(14,u.getStatus().name()); ps.setBoolean(15,u.isVerificacaoDuasEtapas());ps.setBoolean(16,u.isTrocaSenhaObrigatoria());ps.setString(17,u.getUnidade());
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario(); u.setId(rs.getLong("usuario_id")); u.setNome(rs.getString("nome")); u.setCpf(rs.getString("cpf"));
        u.setEmail(rs.getString("email")); u.setSenhaHash(rs.getString("senha_hash")); u.setTelefone(rs.getString("telefone"));
        u.setDataNascimento(rs.getObject("data_nascimento", java.time.LocalDate.class)); u.setCep(rs.getString("cep")); u.setLogradouro(rs.getString("logradouro"));
        u.setNumero(rs.getString("numero")); u.setComplemento(rs.getString("complemento")); u.setBairro(rs.getString("bairro")); u.setCidade(rs.getString("cidade"));
        u.setEstado(rs.getString("estado")); u.setStatus(StatusUsuario.valueOf(rs.getString("status"))); u.setVerificacaoDuasEtapas(rs.getBoolean("verificacao_duas_etapas"));u.setTrocaSenhaObrigatoria(rs.getBoolean("troca_senha_obrigatoria"));u.setUnidade(rs.getString("unidade"));
        u.setUltimoIp(rs.getString("ultimo_ip")); u.setTokenRecuperacaoHash(rs.getString("token_recuperacao_hash"));
        var ultimo=rs.getTimestamp("ultimo_acesso_em"); var expira=rs.getTimestamp("token_recuperacao_expira_em"); var criado=rs.getTimestamp("criado_em"); var atualizado=rs.getTimestamp("atualizado_em");
        u.setUltimoAcessoEm(ultimo==null?null:ultimo.toLocalDateTime()); u.setTokenRecuperacaoExpiraEm(expira==null?null:expira.toLocalDateTime());
        u.setCriadoEm(criado.toLocalDateTime()); u.setAtualizadoEm(atualizado.toLocalDateTime()); return u;
    }
}
