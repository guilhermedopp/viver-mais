package com.dao;

import com.vo.ComunidadeVO;
import com.vo.UsuarioVO;
import java.sql.*;
import java.util.*;

public class ComunidadeDAO {

    public ComunidadeVO salvar(ComunidadeVO com, int criadorId) throws Exception {
        String sql = "INSERT INTO comunidades (nome, descricao, criador_id) VALUES (?,?,?)";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, com.getNome());
            stmt.setString(2, com.getDescricao());
            stmt.setInt(3, criadorId);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) com.setId(rs.getInt(1));
            }
        }
        adicionarMembro(com.getId(), criadorId, "ADMIN");
        return com;
    }

    // Atualiza nome, descrição e/ou foto do grupo (só ADMIN pode chamar)
    public void atualizar(int comunidadeId, String nome, String descricao, String fotoGrupo) throws Exception {
        String sql = "UPDATE comunidades SET nome = ?, descricao = ?, foto_grupo = ? WHERE id = ?";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nome);
            stmt.setString(2, descricao);
            stmt.setString(3, fotoGrupo);   // pode ser null para manter a foto atual
            stmt.setInt(4, comunidadeId);
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new Exception("Erro ao atualizar grupo: " + e.getMessage());
        }
    }

    public List<ComunidadeVO> listarDoUsuario(int usuarioId) throws Exception {
        String sql = "SELECT c.id, c.nome, c.descricao, c.foto_grupo, mc.papel " +
                     "FROM comunidades c JOIN membros_comunidade mc ON c.id = mc.comunidade_id " +
                     "WHERE mc.usuario_id = ? ORDER BY c.nome ASC";
        List<ComunidadeVO> lista = new ArrayList<>();
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ComunidadeVO c = new ComunidadeVO(rs.getInt("id"), rs.getString("nome"), rs.getString("descricao"));
                    c.setFotoGrupo(rs.getString("foto_grupo"));
                    c.setPapel(rs.getString("papel"));
                    lista.add(c);
                }
            }
        }
        return lista;
    }

    public boolean ehMembro(int comunidadeId, int usuarioId) throws Exception {
        String sql = "SELECT 1 FROM membros_comunidade WHERE comunidade_id=? AND usuario_id=?";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, comunidadeId); stmt.setInt(2, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) { return rs.next(); }
        }
    }

    public boolean ehAdmin(int comunidadeId, int usuarioId) throws Exception {
        String sql = "SELECT 1 FROM membros_comunidade WHERE comunidade_id=? AND usuario_id=? AND papel='ADMIN'";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, comunidadeId); stmt.setInt(2, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) { return rs.next(); }
        }
    }

    public void adicionarMembro(int comunidadeId, int usuarioId, String papel) throws Exception {
        String sql = "INSERT IGNORE INTO membros_comunidade (usuario_id, comunidade_id, papel) VALUES (?,?,?)";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, usuarioId); stmt.setInt(2, comunidadeId); stmt.setString(3, papel);
            stmt.executeUpdate();
        }
    }

    public List<UsuarioVO> listarMembros(int comunidadeId) throws Exception {
        String sql = "SELECT u.id, u.nome, u.nickname, u.foto_perfil, mc.papel " +
                     "FROM membros_comunidade mc JOIN usuarios u ON mc.usuario_id = u.id " +
                     "WHERE mc.comunidade_id = ? ORDER BY mc.papel ASC, u.nome ASC";
        List<UsuarioVO> lista = new ArrayList<>();
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, comunidadeId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UsuarioVO u = new UsuarioVO(rs.getInt("id"), rs.getString("nome"),
                            rs.getString("nickname"), "", "", null);
                    u.setFotoPerfil(rs.getString("foto_perfil"));
                    lista.add(u);
                }
            }
        }
        return lista;
    }

    public int convidar(int comunidadeId, int convidanteId, int convidadoId) throws Exception {
        if (ehMembro(comunidadeId, convidadoId))
            throw new Exception("Este usuário já é membro do grupo.");
        String check = "SELECT id FROM convites WHERE comunidade_id=? AND convidado_id=? AND status='PENDENTE'";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement s = conn.prepareStatement(check)) {
            s.setInt(1, comunidadeId); s.setInt(2, convidadoId);
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) throw new Exception("Já existe um convite pendente para esse usuário.");
            }
        }
        String sql = "INSERT INTO convites (comunidade_id, convidante_id, convidado_id) VALUES (?,?,?)";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, comunidadeId); stmt.setInt(2, convidanteId); stmt.setInt(3, convidadoId);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public void responderConvite(int conviteId, int convidadoId, boolean aceitar) throws Exception {
        String sqlBusca = "SELECT comunidade_id, convidado_id FROM convites WHERE id=? AND status='PENDENTE'";
        int comunidadeId = -1;
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sqlBusca)) {
            stmt.setInt(1, conviteId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) throw new Exception("Convite não encontrado ou já respondido.");
                if (rs.getInt("convidado_id") != convidadoId) throw new Exception("Convite não pertence a você.");
                comunidadeId = rs.getInt("comunidade_id");
            }
        }
        String sqlAtualiza = "UPDATE convites SET status=? WHERE id=?";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sqlAtualiza)) {
            stmt.setString(1, aceitar ? "ACEITO" : "RECUSADO"); stmt.setInt(2, conviteId);
            stmt.executeUpdate();
        }
        if (aceitar) adicionarMembro(comunidadeId, convidadoId, "MEMBRO");
    }

    public List<Map<String, Object>> listarConvitesPendentes(int convidadoId) throws Exception {
        String sql = "SELECT cv.id, c.nome AS grupo_nome, u.nome AS quem_convidou " +
                     "FROM convites cv JOIN comunidades c ON cv.comunidade_id = c.id " +
                     "JOIN usuarios u ON cv.convidante_id = u.id " +
                     "WHERE cv.convidado_id=? AND cv.status='PENDENTE'";
        List<Map<String, Object>> lista = new ArrayList<>();
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, convidadoId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("conviteId",    rs.getInt("id"));
                    m.put("grupoNome",    rs.getString("grupo_nome"));
                    m.put("quemConvidou", rs.getString("quem_convidou"));
                    lista.add(m);
                }
            }
        }
        return lista;
    }

    public ComunidadeVO buscarPorId(int id) throws Exception {
        String sql = "SELECT * FROM comunidades WHERE id=?";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ComunidadeVO c = new ComunidadeVO(rs.getInt("id"), rs.getString("nome"), rs.getString("descricao"));
                    c.setFotoGrupo(rs.getString("foto_grupo"));
                    return c;
                }
            }
        }
        return null;
    }
}