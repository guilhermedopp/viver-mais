package com.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.vo.ComunidadeVO;

public class ComunidadeDAO {

    public ComunidadeVO salvar(ComunidadeVO com) throws Exception {
        String sql = "INSERT INTO comunidades (nome, descricao) VALUES (?, ?)";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, com.getNome());
            stmt.setString(2, com.getDescricao());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    com.setId(rs.getInt(1));
                }
            }
            return com;
        } catch (Exception e) {
            throw new Exception("Erro ao criar a comunidade: " + e.getMessage());
        }
    }

    public List<ComunidadeVO> listarTodas() throws Exception {
        String sql = "SELECT * FROM comunidades ORDER BY nome ASC";
        List<ComunidadeVO> lista = new ArrayList<>();
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                ComunidadeVO com = new ComunidadeVO(rs.getInt("id"), rs.getString("nome"), rs.getString("descricao"));
                lista.add(com);
            }
        } catch (Exception e) {
            throw new Exception("Erro ao listar comunidades: " + e.getMessage());
        }
        return lista;
    }
}