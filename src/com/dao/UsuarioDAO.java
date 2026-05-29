package com.dao;

import com.vo.UsuarioVO;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {
    // Simulando o banco de dados em memória para os testes da Semana 2 
    private static List<UsuarioVO> bancoDeUsuarios = new ArrayList<>();
    private static int geradorId = 1;

    public void salvar(UsuarioVO user) {
        // Cria um novo usuário com um ID gerado
        UsuarioVO novoUser = new UsuarioVO(geradorId++, user.getNome(), user.getEmail(), user.getSenha());
        bancoDeUsuarios.add(novoUser);
        System.out.println("Usuário " + novoUser.getNome() + " salvo no banco de dados simulado!");
    }

    // Método essencial para o Login 
    public UsuarioVO buscarPorEmailESenha(String email, String senha) {
        for (UsuarioVO u : bancoDeUsuarios) {
            if (u.getEmail().equals(email) && u.getSenha().equals(senha)) {
                return u; // Encontrou o usuário
            }
        }
        return null; // Não encontrou
    }
}