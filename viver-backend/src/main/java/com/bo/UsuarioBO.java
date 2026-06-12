package com.bo;

import java.time.LocalDate;
import java.time.Period;
import com.dao.PostDAO;
import com.dao.UsuarioDAO;
import com.vo.PostVO;
import com.vo.UsuarioVO;

public class UsuarioBO {
    private PostDAO postDao = new PostDAO();
    private UsuarioDAO dao = new UsuarioDAO();

    public UsuarioVO cadastrar(UsuarioVO vo) throws Exception {
        if (vo.getNome() == null || vo.getNome().trim().isEmpty()) {
            throw new Exception("O nome não pode estar vazio.");
        }
        if (vo.getEmail() == null || !vo.getEmail().contains("@")) {
            throw new Exception("Por favor, insira um e-mail válido com '@'.");
        }
        if (vo.getSenha() == null || vo.getSenha().length() < 3) {
            throw new Exception("Sua senha é muito curta. Use pelo menos 3 caracteres.");
        }
        if (vo.getDataNascimento() == null) {
            throw new Exception("A data de nascimento é obrigatória.");
        }

        // A Mágica da Verificação de Idade
        int idade = Period.between(vo.getDataNascimento(), LocalDate.now()).getYears();
        if (idade < 60) {
            throw new Exception("O VIVER+ é uma comunidade exclusiva para pessoas com 60 anos ou mais.");
        }

        if (dao.buscarPorEmail(vo.getEmail()) != null) {
            throw new Exception("Este e-mail já está cadastrado. Tente fazer login.");
        }

        return dao.salvar(vo);
    }

    public UsuarioVO login(String email, String senha) throws Exception {
        UsuarioVO usuario = dao.buscarPorEmailESenha(email, senha);
        if (usuario == null) {
            throw new Exception("E-mail ou senha incorretos. Tente novamente.");
        }
        return usuario; 
    }

    public boolean seguir(UsuarioVO seguidor, UsuarioVO alvo) throws Exception {
        if (seguidor.getId() == alvo.getId()) {
            throw new Exception("Você não pode seguir a si mesmo!");
        }
        return true; 
    }

    public PostVO criarPostagem(UsuarioVO autor, String texto) throws Exception {
        if (texto == null || texto.trim().isEmpty()) {
            throw new Exception("A sua mensagem não pode estar vazia.");
        }
        
        String textoMinusculo = texto.toLowerCase();
        if (textoMinusculo.contains("http://") || textoMinusculo.contains("https://") || textoMinusculo.contains("clique aqui")) {
            throw new Exception("ALERTA DE SEGURANÇA: Evite links externos para sua proteção.");
        }
        
        PostVO novoPost = new PostVO(0, texto, autor);
        return postDao.salvar(novoPost); 
    }
}