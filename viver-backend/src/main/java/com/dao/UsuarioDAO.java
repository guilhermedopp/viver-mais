package com.dao;

import com.vo.EstatisticasPerfilVO;
import com.vo.UsuarioVO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    public UsuarioVO salvar(UsuarioVO user) throws Exception {
        String sql = "INSERT INTO usuarios (nome, nickname, email, senha, data_nascimento) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getNome());
            stmt.setString(2, user.getNickname());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getSenha());
            stmt.setDate(5, user.getDataNascimento() != null ? java.sql.Date.valueOf(user.getDataNascimento()) : null);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    UsuarioVO salvo = new UsuarioVO(rs.getInt(1), user.getNome(), user.getNickname(),
                            user.getEmail(), user.getSenha(), user.getDataNascimento());
                    salvo.setFotoPerfil(user.getFotoPerfil());
                    return salvo;
                }
            }
            return user;
        } catch (Exception e) {
            throw new Exception("Erro ao salvar: " + e.getMessage());
        }
    }

    // Salva (ou atualiza) usuário que entrou via Google OAuth
    public UsuarioVO salvarOuAtualizarGoogle(String googleId, String nome, String email, String fotoPerfil) throws Exception {
        // Verifica se já existe
        UsuarioVO existente = buscarPorGoogleId(googleId);
        if (existente != null) return existente;

        // Verifica se e-mail já existe sem Google (conta normal)
        existente = buscarPorEmail(email);
        if (existente != null) {
            // Vincula a conta Google à conta existente
            String sql = "UPDATE usuarios SET google_id = ? WHERE id = ?";
            try (Connection conn = ConexaoDB.conectar();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, googleId); stmt.setInt(2, existente.getId());
                stmt.executeUpdate();
            }
            return existente;
        }

        // Cria conta nova via Google (sem senha, sem data nascimento por enquanto)
        String sql = "INSERT INTO usuarios (nome, email, google_id, foto_perfil) VALUES (?, ?, ?, ?)";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, nome);
            stmt.setString(2, email);
            stmt.setString(3, googleId);
            stmt.setString(4, fotoPerfil);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    UsuarioVO u = new UsuarioVO(rs.getInt(1), nome, email, null, null);
                    u.setGoogleId(googleId);
                    u.setFotoPerfil(fotoPerfil);
                    return u;
                }
            }
        }
        return null;
    }

    public UsuarioVO buscarPorId(int id) throws Exception {
        return buscarPor("id = ?", stmt -> stmt.setInt(1, id));
    }

    public UsuarioVO buscarPorEmail(String email) throws Exception {
        return buscarPor("email = ?", stmt -> stmt.setString(1, email));
    }

    public UsuarioVO buscarPorNickname(String nickname) throws Exception {
        return buscarPor("nickname = ?", stmt -> stmt.setString(1, nickname));
    }

    public UsuarioVO buscarPorGoogleId(String googleId) throws Exception {
        return buscarPor("google_id = ?", stmt -> stmt.setString(1, googleId));
    }

    public UsuarioVO buscarPorEmailESenha(String email, String senha) throws Exception {
        String sql = "SELECT * FROM usuarios WHERE email = ? AND senha = ? AND google_id IS NULL";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email); stmt.setString(2, senha);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    public boolean nicknameDisponivel(String nickname, int excluirId) throws Exception {
        String sql = "SELECT 1 FROM usuarios WHERE nickname = ? AND id != ?";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nickname); stmt.setInt(2, excluirId);
            try (ResultSet rs = stmt.executeQuery()) { return !rs.next(); }
        }
    }

    public void atualizarFoto(int usuarioId, String base64) throws Exception {
        executar("UPDATE usuarios SET foto_perfil = ? WHERE id = ?",
                 stmt -> { stmt.setString(1, base64); stmt.setInt(2, usuarioId); });
    }

    public void atualizarNickname(int usuarioId, String nickname) throws Exception {
        executar("UPDATE usuarios SET nickname = ? WHERE id = ?",
                 stmt -> { stmt.setString(1, nickname); stmt.setInt(2, usuarioId); });
    }

    public void atualizarDataNascimento(int usuarioId, java.time.LocalDate data) throws Exception {
        executar("UPDATE usuarios SET data_nascimento = ? WHERE id = ?",
                 stmt -> { stmt.setDate(1, java.sql.Date.valueOf(data)); stmt.setInt(2, usuarioId); });
    }

    public List<UsuarioVO> listarTodos() throws Exception {
        String sql = "SELECT id, nome, nickname, email, foto_perfil FROM usuarios ORDER BY nome ASC";
        List<UsuarioVO> lista = new ArrayList<>();
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                UsuarioVO u = new UsuarioVO(rs.getInt("id"), rs.getString("nome"),
                        rs.getString("nickname"), rs.getString("email"), "", null);
                u.setFotoPerfil(rs.getString("foto_perfil"));
                lista.add(u);
            }
        }
        return lista;
    }

    public EstatisticasPerfilVO buscarEstatisticas(int usuarioId) throws Exception {
        String sql = "SELECT " +
                "(SELECT COUNT(*) FROM postagens WHERE usuario_id = ?) AS posts, " +
                "(SELECT COUNT(*) FROM seguidores WHERE seguido_id  = ?) AS seguidores, " +
                "(SELECT COUNT(*) FROM seguidores WHERE seguidor_id = ?) AS seguindo";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, usuarioId); stmt.setInt(2, usuarioId); stmt.setInt(3, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return new EstatisticasPerfilVO(
                        rs.getInt("posts"), rs.getInt("seguidores"), rs.getInt("seguindo"));
            }
        }
        return new EstatisticasPerfilVO(0, 0, 0);
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    @FunctionalInterface
    private interface PrepSetter { void set(PreparedStatement s) throws Exception; }

    private UsuarioVO buscarPor(String condicao, PrepSetter setter) throws Exception {
        String sql = "SELECT * FROM usuarios WHERE " + condicao;
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            setter.set(stmt);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    private void executar(String sql, PrepSetter setter) throws Exception {
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            setter.set(stmt);
            stmt.executeUpdate();
        }
    }

    private UsuarioVO mapear(ResultSet rs) throws Exception {
        UsuarioVO u = new UsuarioVO(
            rs.getInt("id"), rs.getString("nome"), rs.getString("nickname"),
            rs.getString("email"), rs.getString("senha"),
            rs.getDate("data_nascimento") != null ? rs.getDate("data_nascimento").toLocalDate() : null
        );
        u.setFotoPerfil(rs.getString("foto_perfil"));
        try { u.setGoogleId(rs.getString("google_id")); } catch (Exception ignored) {}
        return u;
    }
}