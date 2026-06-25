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

    // Retorna seguidores mútuos (ambos se seguem)
    public List<UsuarioVO> listarAmigos(int usuarioId) throws Exception {
        String sql = "SELECT u.id, u.nome, u.nickname, u.email, u.foto_perfil " +
                     "FROM usuarios u " +
                     "WHERE EXISTS (SELECT 1 FROM seguidores WHERE seguidor_id = ? AND seguido_id = u.id) " +
                     "  AND EXISTS (SELECT 1 FROM seguidores WHERE seguidor_id = u.id AND seguido_id = ?) " +
                     "ORDER BY u.nome ASC";
        List<UsuarioVO> lista = new ArrayList<>();
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, usuarioId);
            stmt.setInt(2, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UsuarioVO u = new UsuarioVO(rs.getInt("id"), rs.getString("nome"),
                            rs.getString("nickname"), rs.getString("email"), "", null);
                    u.setFotoPerfil(rs.getString("foto_perfil"));
                    lista.add(u);
                }
            }
        } catch (Exception e) {
            throw new Exception("Erro ao listar amigos: " + e.getMessage());
        }
        return lista;
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