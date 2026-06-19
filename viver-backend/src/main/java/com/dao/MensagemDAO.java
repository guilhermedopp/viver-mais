package com.dao;

import com.vo.MensagemVO;
import com.vo.UsuarioVO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MensagemDAO {

    public void enviar(int remetenteId, int destinatarioId, String conteudo) throws Exception {
        String sql = "INSERT INTO mensagens (remetente_id, destinatario_id, conteudo) VALUES (?,?,?)";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, remetenteId);
            stmt.setInt(2, destinatarioId);
            stmt.setString(3, conteudo);
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new Exception("Erro ao enviar mensagem: " + e.getMessage());
        }
    }

    // Conversa entre dois usuários
    public List<MensagemVO> buscarConversa(int usuarioAId, int usuarioBId) throws Exception {
        String sql = "SELECT m.id, m.conteudo, m.lida, m.data_criacao, " +
                     "r.id AS rid, r.nome AS rnome, r.foto_perfil AS rfoto, " +
                     "d.id AS did, d.nome AS dnome, d.foto_perfil AS dfoto " +
                     "FROM mensagens m " +
                     "JOIN usuarios r ON m.remetente_id = r.id " +
                     "JOIN usuarios d ON m.destinatario_id = d.id " +
                     "WHERE (m.remetente_id=? AND m.destinatario_id=?) " +
                     "   OR (m.remetente_id=? AND m.destinatario_id=?) " +
                     "ORDER BY m.data_criacao ASC";
        List<MensagemVO> lista = new ArrayList<>();
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, usuarioAId); stmt.setInt(2, usuarioBId);
            stmt.setInt(3, usuarioBId); stmt.setInt(4, usuarioAId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UsuarioVO rem = new UsuarioVO(rs.getInt("rid"), rs.getString("rnome"), "", "", null);
                    rem.setFotoPerfil(rs.getString("rfoto"));
                    UsuarioVO dest = new UsuarioVO(rs.getInt("did"), rs.getString("dnome"), "", "", null);
                    dest.setFotoPerfil(rs.getString("dfoto"));
                    lista.add(new MensagemVO(rs.getInt("id"), rem, dest, rs.getString("conteudo"),
                            rs.getBoolean("lida"), rs.getTimestamp("data_criacao")));
                }
            }
        }
        return lista;
    }

    // Lista de conversas recentes do usuário (uma entrada por contato)
    public List<UsuarioVO> listarContatos(int usuarioId) throws Exception {
        String sql = "SELECT DISTINCT u.id, u.nome, u.foto_perfil, " +
                     "(SELECT COUNT(*) FROM mensagens m2 WHERE m2.remetente_id = u.id " +
                     " AND m2.destinatario_id = ? AND m2.lida = FALSE) AS nao_lidas " +
                     "FROM mensagens m " +
                     "JOIN usuarios u ON (CASE WHEN m.remetente_id=? THEN m.destinatario_id ELSE m.remetente_id END) = u.id " +
                     "WHERE m.remetente_id=? OR m.destinatario_id=? " +
                     "ORDER BY (SELECT MAX(m3.data_criacao) FROM mensagens m3 WHERE " +
                     " (m3.remetente_id=? AND m3.destinatario_id=u.id) OR " +
                     " (m3.remetente_id=u.id AND m3.destinatario_id=?)) DESC";
        List<UsuarioVO> lista = new ArrayList<>();
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, usuarioId); stmt.setInt(2, usuarioId);
            stmt.setInt(3, usuarioId); stmt.setInt(4, usuarioId);
            stmt.setInt(5, usuarioId); stmt.setInt(6, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UsuarioVO u = new UsuarioVO(rs.getInt("id"), rs.getString("nome"), "", "", null);
                    u.setFotoPerfil(rs.getString("foto_perfil"));
                    lista.add(u);
                }
            }
        }
        return lista;
    }

    public void marcarComoLidas(int remetenteId, int destinatarioId) throws Exception {
        String sql = "UPDATE mensagens SET lida=TRUE WHERE remetente_id=? AND destinatario_id=?";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, remetenteId); stmt.setInt(2, destinatarioId);
            stmt.executeUpdate();
        }
    }

    public int contarNaoLidas(int destinatarioId) throws Exception {
        String sql = "SELECT COUNT(*) FROM mensagens WHERE destinatario_id=? AND lida=FALSE";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, destinatarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }
}