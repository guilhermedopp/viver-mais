package com.bo;

import com.vo.UsuarioVO;
import com.vo.PostVO;
import com.dao.UsuarioDAO;

public class UsuarioBO {
    private UsuarioDAO dao = new UsuarioDAO();

    public UsuarioVO cadastrar(UsuarioVO vo) throws Exception {
        // Validações originais mantidas
        if (vo.getNome() == null || vo.getNome().trim().isEmpty()) {
            throw new Exception("O nome não pode estar vazio.");
        }
        if (vo.getEmail() == null || !vo.getEmail().contains("@")) {
            throw new Exception("Por favor, insira um e-mail válido com '@'.");
        }
        if (vo.getSenha() == null || vo.getSenha().length() < 3) {
            throw new Exception("Sua senha é muito curta. Use pelo menos 3 caracteres.");
        }
        
        dao.salvar(vo);
        return vo; // Retorna o usuário para o Frontend saber quem foi cadastrado
    }

    public UsuarioVO login(String email, String senha) throws Exception {
        UsuarioVO usuario = dao.buscarPorEmailESenha(email, senha);
        if (usuario == null) {
            throw new Exception("E-mail ou senha incorretos. Tente novamente.");
        }
        return usuario; // Retorna os dados do usuário para o Frontend
    }

    // Retorna 'true' para a API confirmar que a ação deu certo
    public boolean seguir(UsuarioVO seguidor, UsuarioVO alvo) throws Exception {
        if (seguidor.getId() == alvo.getId()) {
            throw new Exception("Você não pode seguir a si mesmo!");
        }
        return true; 
    }

    // Retorna o objeto PostVO criado para a API exibir no feed do celular
    public PostVO criarPostagem(UsuarioVO autor, String texto) throws Exception {
        if (texto.contains("http://") || texto.contains("clique aqui")) {
            throw new Exception("ALERTA DE SEGURANÇA: Evite links externos para sua proteção.");
        }
        PostVO novoPost = new PostVO(1, texto, autor);
        return novoPost; 
    }
}