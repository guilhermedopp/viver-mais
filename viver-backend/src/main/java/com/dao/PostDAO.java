package com.dao;

import com.vo.PostVO;
import com.vo.UsuarioVO;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PostDAO {

    public PostVO salvar(PostVO post) throws Exception {
        String sql = "INSERT INTO postagens (conteudo, usuario_id) VALUES (?, ?)";
        
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
             
            stmt.setString(1, post.getConteudo());
            stmt.setInt(2, post.getAutor().getId());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    // Retorna um novo objeto com o ID correto gerado pela base de dados
                    return new PostVO(rs.getInt(1), post.getConteudo(), post.getAutor());
                }
            }
            return post;
        } catch (Exception e) {
            throw new Exception("Erro ao salvar a postagem: " + e.getMessage());
        }
    }

    public List<PostVO> listarTodos() throws Exception {
        // Atualizado para puxar também a data de nascimento do utilizador
        String sql = "SELECT p.id, p.conteudo, p.data_criacao, u.id AS user_id, u.nome, u.email, u.data_nascimento " +
                     "FROM postagens p " +
                     "INNER JOIN usuarios u ON p.usuario_id = u.id " +
                     "ORDER BY p.data_criacao DESC";
                     
        List<PostVO> lista = new ArrayList<>();

        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                // Prepara a data de nascimento para não dar erro se vier vazia
                LocalDate dataNasc = rs.getDate("data_nascimento") != null ? rs.getDate("data_nascimento").toLocalDate() : null;
                
                UsuarioVO autor = new UsuarioVO(
                    rs.getInt("user_id"),
                    rs.getString("nome"),
                    rs.getString("email"),
                    "", // Oculta a senha por segurança no Feed
                    dataNasc // <-- Passando o novo campo obrigatório
                );
                
                PostVO post = new PostVO(rs.getInt("id"), rs.getString("conteudo"), autor);
                // Define a data que veio da BD
                post.setData(rs.getTimestamp("data_criacao")); 
                lista.add(post);
            }
        } catch (Exception e) {
            throw new Exception("Erro ao listar postagens: " + e.getMessage());
        }
        return lista;
    }
}