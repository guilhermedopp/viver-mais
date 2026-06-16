package com.dao;

import com.vo.UsuarioVO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    public UsuarioVO salvar(UsuarioVO user) throws Exception {
        String sql = "INSERT INTO usuarios (nome, email, senha, data_nascimento) VALUES (?, ?, ?, ?)";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getNome());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getSenha());
            stmt.setDate(4, java.sql.Date.valueOf(user.getDataNascimento()));
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return new UsuarioVO(rs.getInt(1), user.getNome(), user.getEmail(), user.getSenha(), user.getDataNascimento());
                }
            }
            return user;
        } catch (Exception e) {
            throw new Exception("Erro ao salvar: " + e.getMessage());
        }
    }

    public UsuarioVO buscarPorId(int id) throws Exception {
        String sql = "SELECT * FROM usuarios WHERE id = ?";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (Exception e) {
            throw new Exception("Erro ao buscar usuário: " + e.getMessage());
        }
        return null;
    }

    public UsuarioVO buscarPorEmail(String email) throws Exception {
        String sql = "SELECT * FROM usuarios WHERE email = ?";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (Exception e) {
            throw new Exception("Erro: " + e.getMessage());
        }
        return null;
    }

    public UsuarioVO buscarPorEmailESenha(String email, String senha) throws Exception {
        String sql = "SELECT * FROM usuarios WHERE email = ? AND senha = ?";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, senha);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (Exception e) {
            throw new Exception("Erro: " + e.getMessage());
        }
        return null;
    }

    // Atualiza a foto de perfil (base64)
    public void atualizarFoto(int usuarioId, String base64) throws Exception {
        String sql = "UPDATE usuarios SET foto_perfil = ? WHERE id = ?";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, base64);
            stmt.setInt(2, usuarioId);
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new Exception("Erro ao salvar foto: " + e.getMessage());
        }
    }

    // Lista todos os usuários (para a aba de "Ver Pessoas")
    public List<UsuarioVO> listarTodos() throws Exception {
        String sql = "SELECT id, nome, email, foto_perfil FROM usuarios ORDER BY nome ASC";
        List<UsuarioVO> lista = new ArrayList<>();
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                UsuarioVO u = new UsuarioVO(rs.getInt("id"), rs.getString("nome"), rs.getString("email"), "", null);
                u.setFotoPerfil(rs.getString("foto_perfil"));
                lista.add(u);
            }
        } catch (Exception e) {
            throw new Exception("Erro ao listar usuários: " + e.getMessage());
        }
        return lista;
    }

    private UsuarioVO mapear(ResultSet rs) throws Exception {
        UsuarioVO u = new UsuarioVO(
            rs.getInt("id"), rs.getString("nome"), rs.getString("email"),
            rs.getString("senha"),
            rs.getDate("data_nascimento") != null ? rs.getDate("data_nascimento").toLocalDate() : null
        );
        u.setFotoPerfil(rs.getString("foto_perfil"));
        return u;
    }
}