package com.dao;

import com.vo.UsuarioVO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SeguidorDAO {

    public void seguir(int seguidorId, int seguidoId) throws Exception {
        String sql = "INSERT IGNORE INTO seguidores (seguidor_id, seguido_id) VALUES (?, ?)";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, seguidorId);
            stmt.setInt(2, seguidoId);
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new Exception("Erro ao seguir: " + e.getMessage());
        }
    }

    public void deixarDeSeguir(int seguidorId, int seguidoId) throws Exception {
        String sql = "DELETE FROM seguidores WHERE seguidor_id = ? AND seguido_id = ?";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, seguidorId);
            stmt.setInt(2, seguidoId);
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new Exception("Erro ao deixar de seguir: " + e.getMessage());
        }
    }

    public boolean jaSegue(int seguidorId, int seguidoId) throws Exception {
        String sql = "SELECT 1 FROM seguidores WHERE seguidor_id = ? AND seguido_id = ?";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, seguidorId);
            stmt.setInt(2, seguidoId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new Exception("Erro ao verificar seguidor: " + e.getMessage());
        }
    }

    // Lista quem segue o usuário indicado (usada pelo Observer para notificar)
    public List<UsuarioVO> listarSeguidores(int usuarioId) throws Exception {
        String sql = "SELECT u.id, u.nome, u.email FROM seguidores s " +
                     "INNER JOIN usuarios u ON s.seguidor_id = u.id " +
                     "WHERE s.seguido_id = ?";
        List<UsuarioVO> lista = new ArrayList<>();
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new UsuarioVO(
                        rs.getInt("id"), rs.getString("nome"),
                        rs.getString("email"), "", null
                    ));
                }
            }
        } catch (Exception e) {
            throw new Exception("Erro ao listar seguidores: " + e.getMessage());
        }
        return lista;
    }
}