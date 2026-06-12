package com.dao;

import com.vo.UsuarioVO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Date;

public class UsuarioDAO {

    public UsuarioVO salvar(UsuarioVO user) throws Exception {
        String sql = "INSERT INTO usuarios (nome, email, senha, data_nascimento) VALUES (?, ?, ?, ?)";

        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getNome());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getSenha());
            stmt.setDate(4, Date.valueOf(user.getDataNascimento())); // Converte LocalDate para Date do SQL

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return new UsuarioVO(rs.getInt(1), user.getNome(), user.getEmail(), user.getSenha(), user.getDataNascimento());
                }
            }
            return user;
        } catch (Exception e) {
            throw new Exception("Erro ao salvar no banco: " + e.getMessage());
        }
    }

    public UsuarioVO buscarPorEmail(String email) throws Exception {
        String sql = "SELECT * FROM usuarios WHERE email = ?";
        try (Connection conn = ConexaoDB.conectar(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new UsuarioVO(rs.getInt("id"), rs.getString("nome"), rs.getString("email"), rs.getString("senha"), rs.getDate("data_nascimento") != null ? rs.getDate("data_nascimento").toLocalDate() : null);
                }
            }
        } catch (Exception e) {
            throw new Exception("Erro ao consultar o banco de dados: " + e.getMessage());
        }
        return null;
    }

    public UsuarioVO buscarPorEmailESenha(String email, String senha) throws Exception {
        String sql = "SELECT * FROM usuarios WHERE email = ? AND senha = ?";
        try (Connection conn = ConexaoDB.conectar(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, senha);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new UsuarioVO(rs.getInt("id"), rs.getString("nome"), rs.getString("email"), rs.getString("senha"), rs.getDate("data_nascimento") != null ? rs.getDate("data_nascimento").toLocalDate() : null);
                }
            }
        } catch (Exception e) {
            throw new Exception("Erro ao consultar o banco de dados: " + e.getMessage());
        }
        return null; 
    }
}