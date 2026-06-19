package com.dao;

import com.vo.MensagemGrupoVO;
import com.vo.UsuarioVO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MensagemGrupoDAO {

    public void enviar(int comunidadeId, int usuarioId, String conteudo) throws Exception {
        String sql = "INSERT INTO mensagens_grupo (comunidade_id, usuario_id, conteudo) VALUES (?,?,?)";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, comunidadeId);
            stmt.setInt(2, usuarioId);
            stmt.setString(3, conteudo);
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new Exception("Erro ao enviar mensagem no grupo: " + e.getMessage());
        }
    }

    public List<MensagemGrupoVO> listarPorGrupo(int comunidadeId) throws Exception {
        String sql = "SELECT mg.id, mg.conteudo, mg.data_criacao, " +
                     "u.id AS uid, u.nome, u.foto_perfil " +
                     "FROM mensagens_grupo mg " +
                     "JOIN usuarios u ON mg.usuario_id = u.id " +
                     "WHERE mg.comunidade_id = ? " +
                     "ORDER BY mg.data_criacao ASC LIMIT 100";
        List<MensagemGrupoVO> lista = new ArrayList<>();
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, comunidadeId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UsuarioVO autor = new UsuarioVO(rs.getInt("uid"), rs.getString("nome"), "", "", null);
                    autor.setFotoPerfil(rs.getString("foto_perfil"));
                    lista.add(new MensagemGrupoVO(rs.getInt("id"), comunidadeId, autor,
                            rs.getString("conteudo"), rs.getTimestamp("data_criacao")));
                }
            }
        }
        return lista;
    }
}