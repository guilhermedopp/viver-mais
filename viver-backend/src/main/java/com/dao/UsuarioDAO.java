package com.dao;

import com.vo.UsuarioVO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UsuarioDAO {

    public void salvar(UsuarioVO user) throws Exception {
        String sql = "INSERT INTO usuarios (nome, email, senha) VALUES (?, ?, ?)";
        
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            stmt.setString(1, user.getNome());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getSenha());
            
            stmt.executeUpdate(); 
        } catch (Exception e) {
            throw new Exception("Erro ao salvar no banco: " + e.getMessage());
        }
    }

    public UsuarioVO buscarPorEmailESenha(String email, String senha) throws Exception {
        String sql = "SELECT * FROM usuarios WHERE email = ? AND senha = ?";
        UsuarioVO usuarioEncontrado = null;
        
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            stmt.setString(1, email);
            stmt.setString(2, senha);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    usuarioEncontrado = new UsuarioVO(
                        rs.getInt("id"),
                        rs.getString("nome"),
                        rs.getString("email"),
                        rs.getString("senha")
                    );
                }
            }
        } catch (Exception e) {
            throw new Exception("Erro ao consultar o banco de dados: " + e.getMessage());
        }
        
        return usuarioEncontrado; 
    }
}