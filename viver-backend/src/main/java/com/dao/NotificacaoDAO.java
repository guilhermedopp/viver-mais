package com.dao;

import com.vo.NotificacaoVO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificacaoDAO {

    // Salva uma notificação para um usuário específico
    public void salvar(int usuarioId, String mensagem) throws Exception {
        String sql = "INSERT INTO notificacoes (usuario_id, mensagem) VALUES (?, ?)";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, usuarioId);
            stmt.setString(2, mensagem);
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new Exception("Erro ao salvar notificação: " + e.getMessage());
        }
    }

    // Lista todas as notificações de um usuário (mais recentes primeiro)
    public List<NotificacaoVO> listarPorUsuario(int usuarioId) throws Exception {
        String sql = "SELECT * FROM notificacoes WHERE usuario_id = ? ORDER BY data_criacao DESC LIMIT 30";
        List<NotificacaoVO> lista = new ArrayList<>();
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new NotificacaoVO(
                        rs.getInt("id"),
                        rs.getString("mensagem"),
                        rs.getBoolean("lida"),
                        rs.getTimestamp("data_criacao"),
                        usuarioId
                    ));
                }
            }
        } catch (Exception e) {
            throw new Exception("Erro ao listar notificações: " + e.getMessage());
        }
        return lista;
    }

    // Conta notificações não lidas (para o badge na barra de navegação)
    public int contarNaoLidas(int usuarioId) throws Exception {
        String sql = "SELECT COUNT(*) AS total FROM notificacoes WHERE usuario_id = ? AND lida = FALSE";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("total");
            }
        } catch (Exception e) {
            throw new Exception("Erro ao contar notificações: " + e.getMessage());
        }
        return 0;
    }

    // Marca todas as notificações do usuário como lidas
    public void marcarTodasComoLidas(int usuarioId) throws Exception {
        String sql = "UPDATE notificacoes SET lida = TRUE WHERE usuario_id = ?";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, usuarioId);
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new Exception("Erro ao marcar notificações: " + e.getMessage());
        }
    }
}