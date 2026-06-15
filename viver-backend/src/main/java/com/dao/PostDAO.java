package com.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.vo.ComunidadeVO;
import com.vo.PostVO;
import com.vo.UsuarioVO;

public class PostDAO {

    public PostVO salvar(PostVO post) throws Exception {
        String sql = "INSERT INTO postagens (conteudo, usuario_id, destino_tipo, destino_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, post.getConteudo());
            stmt.setInt(2, post.getAutor().getId());
            stmt.setString(3, post.getDestinoTipo()); // 'USUARIO' ou 'COMUNIDADE'
            stmt.setInt(4, post.getDestino().getId()); // Tratamento Polimórfico do ID
            
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    post.setId(rs.getInt(1));
                    return post;
                }
            }
            return post;
        } catch (Exception e) { 
            throw new Exception("Erro ao salvar a postagem: " + e.getMessage()); 
        }
    }

    public List<PostVO> listarTodos() throws Exception {
        String sql = "SELECT p.id, p.conteudo, p.data_criacao, p.destino_tipo, p.destino_id, " +
                     "u.id AS user_id, u.nome, u.email, u.data_nascimento " +
                     "FROM postagens p INNER JOIN usuarios u ON p.usuario_id = u.id ORDER BY p.data_criacao DESC";
        List<PostVO> lista = new ArrayList<>();
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                LocalDate dataNasc = rs.getDate("data_nascimento") != null ? rs.getDate("data_nascimento").toLocalDate() : null;
                UsuarioVO autor = new UsuarioVO(rs.getInt("user_id"), rs.getString("nome"), rs.getString("email"), "", dataNasc);
                
                PostVO post = new PostVO(rs.getInt("id"), rs.getString("conteudo"), autor);
                
                // Correção do mapeamento de data para java.util.Date
                Timestamp timestamp = rs.getTimestamp("data_criacao");
                if (timestamp != null) {
                    post.setData(new java.util.Date(timestamp.getTime()));
                }
                
                post.setDestinoTipo(rs.getString("destino_tipo"));

                // Restauração Polimórfica do Destino
                int destinoId = rs.getInt("destino_id");
                if ("COMUNIDADE".equals(post.getDestinoTipo())) {
                    ComunidadeVO com = buscarComunidadePorId(destinoId);
                    post.setDestino(com);
                } else {
                    if (destinoId == autor.getId()) {
                        post.setDestino(autor);
                    } else {
                        post.setDestino(buscarUsuarioBasicoPorId(destinoId));
                    }
                }

                // Carrega os comentários/respostas vinculados ao post
                post.setRespostas(buscarRespostasPorPost(post.getId()));
                
                lista.add(post);
            }
        } catch (Exception e) { 
            throw new Exception("Erro ao listar postagens: " + e.getMessage()); 
        }
        return lista;
    }

    public List<com.vo.RespostaVO> buscarRespostasPorPost(int postId) throws Exception {
        String sql = "SELECT r.id, r.conteudo, r.data_criacao, u.id AS user_id, u.nome, u.email " +
                     "FROM respostas r INNER JOIN usuarios u ON r.usuario_id = u.id " +
                     "WHERE r.post_id = ? ORDER BY r.data_criacao ASC";
        List<com.vo.RespostaVO> lista = new ArrayList<>();
        try (Connection conn = ConexaoDB.conectar(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, postId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UsuarioVO autor = new UsuarioVO(rs.getInt("user_id"), rs.getString("nome"), rs.getString("email"), "", null);
                    com.vo.RespostaVO resp = new com.vo.RespostaVO(rs.getInt("id"), rs.getString("conteudo"), autor, postId);
                    
                    Timestamp timestamp = rs.getTimestamp("data_criacao");
                    if (timestamp != null) {
                        resp.setData(new java.util.Date(timestamp.getTime()));
                    }
                    
                    lista.add(resp);
                }
            }
        }
        return lista;
    }

    // Métodos auxiliares para preenchimento polimórfico
    private ComunidadeVO buscarComunidadePorId(int id) {
        String sql = "SELECT * FROM comunidades WHERE id = ?";
        try (Connection conn = ConexaoDB.conectar(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new ComunidadeVO(rs.getInt("id"), rs.getString("nome"), rs.getString("descricao"));
                }
            }
        } catch (Exception e) {} // Correção do caractere corrompido aqui
        return new ComunidadeVO(id, "Comunidade Antiga", "");
    }

    private UsuarioVO buscarUsuarioBasicoPorId(int id) {
        String sql = "SELECT id, nome, email FROM usuarios WHERE id = ?";
        try (Connection conn = ConexaoDB.conectar(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new UsuarioVO(rs.getInt("id"), rs.getString("nome"), rs.getString("email"), "", null);
                }
            }
        } catch (Exception e) {}
        return null;
    }

    public boolean alternarCurtida(int usuarioId, int postId) throws Exception {
        String checkSql = "SELECT * FROM curtidas WHERE usuario_id = ? AND post_id = ?";
        try (Connection conn = ConexaoDB.conectar(); PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setInt(1, usuarioId);
            checkStmt.setInt(2, postId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    String deleteSql = "DELETE FROM curtidas WHERE usuario_id = ? AND post_id = ?";
                    try (PreparedStatement delStmt = conn.prepareStatement(deleteSql)) {
                        delStmt.setInt(1, usuarioId);
                        delStmt.setInt(2, postId);
                        delStmt.executeUpdate();
                    }
                    return false;
                } else {
                    String insertSql = "INSERT INTO curtidas (usuario_id, post_id) VALUES (?, ?)";
                    try (PreparedStatement insStmt = conn.prepareStatement(insertSql)) {
                        insStmt.setInt(1, usuarioId);
                        insStmt.setInt(2, postId);
                        insStmt.executeUpdate();
                    }
                    return true;
                }
            }
        } catch (Exception e) { 
            throw new Exception("Erro ao processar curtida: " + e.getMessage()); 
        }
    }

    public void salvarResposta(int usuarioId, int postId, String conteudo) throws Exception {
        String sql = "INSERT INTO respostas (conteudo, usuario_id, post_id) VALUES (?, ?, ?)";
        try (Connection conn = ConexaoDB.conectar(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, conteudo);
            stmt.setInt(2, usuarioId);
            stmt.setInt(3, postId);
            stmt.executeUpdate();
        } catch (Exception e) { 
            throw new Exception("Erro ao salvar comentário: " + e.getMessage()); 
        }
    }
}