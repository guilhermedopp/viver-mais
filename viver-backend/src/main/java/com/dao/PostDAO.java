package com.dao;

import com.vo.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class PostDAO {

    public PostVO salvar(PostVO post) throws Exception {
        String sql = "INSERT INTO postagens (conteudo, usuario_id, destino_tipo, destino_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, post.getConteudo());
            stmt.setInt(2, post.getAutor().getId());
            stmt.setString(3, post.getDestinoTipo() != null ? post.getDestinoTipo() : "USUARIO");
            stmt.setInt(4, post.getDestino() != null ? post.getDestino().getId() : post.getAutor().getId());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) { post.setId(rs.getInt(1)); }
            }
            return post;
        } catch (Exception e) {
            throw new Exception("Erro ao salvar post: " + e.getMessage());
        }
    }

    // Busca um post pelo ID (usado para notificações de comentário)
    public PostVO buscarPorId(int postId) throws Exception {
        String sql = "SELECT p.id, p.conteudo, p.data_criacao, p.destino_tipo, p.destino_id, " +
                     "u.id AS uid, u.nome, u.email FROM postagens p " +
                     "INNER JOIN usuarios u ON p.usuario_id = u.id WHERE p.id = ?";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, postId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    UsuarioVO autor = new UsuarioVO(rs.getInt("uid"), rs.getString("nome"), rs.getString("email"), "", null);
                    PostVO post = new PostVO(rs.getInt("id"), rs.getString("conteudo"), autor);
                    post.setDestinoTipo(rs.getString("destino_tipo"));
                    return post;
                }
            }
        }
        return null;
    }

    // Lista posts com curtidas, respostas e flag de visto pelo usuário logado
    public List<PostVO> listarTodos(int usuarioLogadoId) throws Exception {
        String sql = "SELECT p.id, p.conteudo, p.data_criacao, p.destino_tipo, p.destino_id, " +
                     "u.id AS uid, u.nome, u.email, u.foto_perfil, u.data_nascimento, " +
                     "(SELECT COUNT(*) FROM curtidas c WHERE c.post_id = p.id) AS total_curtidas, " +
                     "(SELECT COUNT(*) FROM curtidas c WHERE c.post_id = p.id AND c.usuario_id = ?) AS eu_curto, " +
                     "(SELECT COUNT(*) FROM visualizacoes v WHERE v.post_id = p.id AND v.usuario_id = ?) AS eu_vi " +
                     "FROM postagens p INNER JOIN usuarios u ON p.usuario_id = u.id " +
                     "ORDER BY p.data_criacao DESC";

        List<PostVO> lista = new ArrayList<>();
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, usuarioLogadoId);
            stmt.setInt(2, usuarioLogadoId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LocalDate dataNasc = rs.getDate("data_nascimento") != null
                            ? rs.getDate("data_nascimento").toLocalDate() : null;
                    UsuarioVO autor = new UsuarioVO(
                        rs.getInt("uid"), rs.getString("nome"), rs.getString("email"), "", dataNasc);
                    autor.setFotoPerfil(rs.getString("foto_perfil"));

                    PostVO post = new PostVO(rs.getInt("id"), rs.getString("conteudo"), autor);
                    Timestamp ts = rs.getTimestamp("data_criacao");
                    if (ts != null) post.setData(new java.util.Date(ts.getTime()));
                    post.setDestinoTipo(rs.getString("destino_tipo"));
                    post.setTotalCurtidas(rs.getInt("total_curtidas"));
                    post.setCurtidoPeloUsuario(rs.getInt("eu_curto") > 0);
                    post.setVisto(rs.getInt("eu_vi") > 0);
                    post.setRespostas(buscarRespostasPorPost(post.getId()));
                    lista.add(post);
                }
            }
        } catch (Exception e) {
            throw new Exception("Erro ao listar posts: " + e.getMessage());
        }
        return lista;
    }

    // Mantém retrocompatibilidade (sem usuário logado)
    public List<PostVO> listarTodos() throws Exception {
        return listarTodos(0);
    }

    // Lista posts de um usuário específico (para o perfil)
    public List<PostVO> listarPorUsuario(int usuarioId) throws Exception {
        String sql = "SELECT p.id, p.conteudo, p.data_criacao, " +
                     "u.id AS uid, u.nome, u.email, u.foto_perfil " +
                     "FROM postagens p INNER JOIN usuarios u ON p.usuario_id = u.id " +
                     "WHERE p.usuario_id = ? ORDER BY p.data_criacao DESC";
        List<PostVO> lista = new ArrayList<>();
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UsuarioVO autor = new UsuarioVO(rs.getInt("uid"), rs.getString("nome"), rs.getString("email"), "", null);
                    autor.setFotoPerfil(rs.getString("foto_perfil"));
                    PostVO post = new PostVO(rs.getInt("id"), rs.getString("conteudo"), autor);
                    Timestamp ts = rs.getTimestamp("data_criacao");
                    if (ts != null) post.setData(new java.util.Date(ts.getTime()));
                    lista.add(post);
                }
            }
        }
        return lista;
    }

    public List<RespostaVO> buscarRespostasPorPost(int postId) throws Exception {
        String sql = "SELECT r.id, r.conteudo, r.data_criacao, u.id AS uid, u.nome, u.email, u.foto_perfil " +
                     "FROM respostas r INNER JOIN usuarios u ON r.usuario_id = u.id " +
                     "WHERE r.post_id = ? ORDER BY r.data_criacao ASC";
        List<RespostaVO> lista = new ArrayList<>();
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, postId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UsuarioVO autor = new UsuarioVO(rs.getInt("uid"), rs.getString("nome"), rs.getString("email"), "", null);
                    autor.setFotoPerfil(rs.getString("foto_perfil"));
                    RespostaVO r = new RespostaVO(rs.getInt("id"), rs.getString("conteudo"), autor, postId);
                    Timestamp ts = rs.getTimestamp("data_criacao");
                    if (ts != null) r.setData(new java.util.Date(ts.getTime()));
                    lista.add(r);
                }
            }
        }
        return lista;
    }

    public boolean alternarCurtida(int usuarioId, int postId) throws Exception {
        String check = "SELECT 1 FROM curtidas WHERE usuario_id = ? AND post_id = ?";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement s = conn.prepareStatement(check)) {
            s.setInt(1, usuarioId); s.setInt(2, postId);
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    try (PreparedStatement d = conn.prepareStatement(
                            "DELETE FROM curtidas WHERE usuario_id=? AND post_id=?")) {
                        d.setInt(1, usuarioId); d.setInt(2, postId); d.executeUpdate();
                    }
                    return false;
                } else {
                    try (PreparedStatement i = conn.prepareStatement(
                            "INSERT INTO curtidas (usuario_id, post_id) VALUES (?,?)")) {
                        i.setInt(1, usuarioId); i.setInt(2, postId); i.executeUpdate();
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            throw new Exception("Erro ao processar curtida: " + e.getMessage());
        }
    }

    public void salvarResposta(int usuarioId, int postId, String conteudo) throws Exception {
        String sql = "INSERT INTO respostas (conteudo, usuario_id, post_id) VALUES (?,?,?)";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, conteudo); stmt.setInt(2, usuarioId); stmt.setInt(3, postId);
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new Exception("Erro ao salvar comentário: " + e.getMessage());
        }
    }

    // Registra que o usuário viu o post (para o filtro vistos/não vistos)
    public void marcarComoVisto(int usuarioId, int postId) throws Exception {
        String sql = "INSERT IGNORE INTO visualizacoes (usuario_id, post_id) VALUES (?,?)";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, usuarioId); stmt.setInt(2, postId);
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new Exception("Erro ao marcar visualização: " + e.getMessage());
        }
    }
}