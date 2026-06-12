package com.bo;

import com.dao.PostDAO;
import com.dao.UsuarioDAO; // Certifique-se de importar o PostDAO se estiver em pacotes diferentes
import com.vo.PostVO;
import com.vo.UsuarioVO;

public class UsuarioBO {
    private PostDAO postDao = new PostDAO();
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

        // Verifica se já existe uma conta com este e-mail
        if (dao.buscarPorEmail(vo.getEmail()) != null) {
            throw new Exception("Este e-mail já está cadastrado. Tente fazer login.");
        }

        // Salva e retorna o usuário já com o ID gerado pelo banco
        return dao.salvar(vo);
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
        // 1. Validação de mensagem vazia (parênteses corrigidos)
        if (texto == null || texto.trim().isEmpty()) {
            throw new Exception("A sua mensagem não pode estar vazia.");
        }
        
        // 2. Alerta de segurança contra links externos e termos suspeitos (sintaxe e ortografia corrigidas)
        String textoMinusculo = texto.toLowerCase();
        if (textoMinusculo.contains("http://") || textoMinusculo.contains("https://") || textoMinusculo.contains("clique aqui")) {
            throw new Exception("ALERTA DE SEGURANÇA: Evite links externos para sua proteção.");
        }
        
        // 3. Criação do objeto utilizando a classe correta (PostVO)
        PostVO novoPost = new PostVO(0, texto, autor);
        
        // --- ATIVAÇÃO OPCIONAL DO OBSERVER (ETAPA 3) ---
        // Se já quiser deixar o Observer ativo para simular no console:
        // com.observer.Observer amigoFicticio = new com.observer.Seguidor(new UsuarioVO(99, "Dona Antónia", "antonia@viver.com", ""));
        // autor.adicionarSeguidor(amigoFicticio);
        // autor.notificarSeguidores("publicou um novo momento: \"" + texto + "\"");
        // ------------------------------------------------
        
        // 4. Salva usando a variável correta (postDao) e retorna o resultado
        return postDao.salvar(novoPost); 
    }
}
