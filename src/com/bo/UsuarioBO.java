package com.bo;

import com.vo.UsuarioVO;
import com.vo.PostVO;
import com.dao.UsuarioDAO;
import java.util.ArrayList;
import java.util.List;

public class UsuarioBO {
    private UsuarioDAO dao = new UsuarioDAO();
    private List<UsuarioVO> seguidores = new ArrayList<>();

    // --- MÉTODOS DA SEMANA 2 (Gestão de Usuários) ---

    public void cadastrar(UsuarioVO vo) throws Exception {
        // Validação de dados [cite: 183]
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
    }

    public UsuarioVO login(String email, String senha) throws Exception {
        UsuarioVO usuario = dao.buscarPorEmailESenha(email, senha);
        if (usuario == null) {
            throw new Exception("E-mail ou senha incorretos. Tente novamente."); // Tratamento de erro [cite: 182]
        }
        return usuario;
    }

    // --- MÉTODOS ANTIGOS (Regras de Negócio do Feed e Sociais) ---

    // Regra de Negócio: Usuário não pode seguir a si mesmo [cite: 188]
    public void seguir(UsuarioVO seguidor, UsuarioVO alvo) throws Exception {
        if (seguidor.getId() == alvo.getId()) {
            throw new Exception("Você não pode seguir a si mesmo!");
        }
        System.out.println(seguidor.getNome() + " agora segue " + alvo.getNome());
    }

    // Regra de Negócio: Prevenção a golpes [cite: 123, 131]
    public void criarPostagem(UsuarioVO autor, String texto) throws Exception {
        if (texto.contains("http://") || texto.contains("clique aqui")) {
            throw new Exception("ALERTA DE SEGURANÇA: Evite links externos para sua proteção.");
        }
        PostVO novoPost = new PostVO(1, texto, autor);
        System.out.println("Postagem criada por: " + autor.getNome());
    }
}