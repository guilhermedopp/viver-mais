package com.bo;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dao.NotificacaoDAO;
import com.dao.PostDAO;
import com.dao.SeguidorDAO;
import com.dao.UsuarioDAO;
import com.observer.Seguidor;
import com.vo.PostVO;
import com.vo.UsuarioVO;

public class UsuarioBO {
    private PostDAO      postDao      = new PostDAO();
    private UsuarioDAO   dao          = new UsuarioDAO();
    private SeguidorDAO  seguidorDao  = new SeguidorDAO();
    private NotificacaoDAO notifDao   = new NotificacaoDAO();

    // ── Simulação de consulta à Receita Federal ───────────────────────
    private boolean consultarReceitaFederal(String cpf, LocalDate dataNasc) {
        Map<String, LocalDate> base = new HashMap<>();
        base.put("12345678900", LocalDate.of(1955, 5, 20));
        base.put("98765432100", LocalDate.of(1960, 10, 15));
        // Para testes: qualquer CPF válido que não esteja na base ainda é aceito
        // (remover a linha abaixo em produção real)
        if (!base.containsKey(cpf)) return true;
        return base.get(cpf).equals(dataNasc);
    }

    // ── Cadastro ──────────────────────────────────────────────────────
    public UsuarioVO cadastrar(UsuarioVO vo, String cpf) throws Exception {
        if (vo.getNome() == null || vo.getNome().trim().isEmpty())
            throw new Exception("O nome não pode estar vazio.");
        if (vo.getEmail() == null || !vo.getEmail().contains("@"))
            throw new Exception("Insira um e-mail válido.");
        if (vo.getSenha() == null || vo.getSenha().length() < 3)
            throw new Exception("Senha muito curta. Mínimo 3 caracteres.");
        if (vo.getDataNascimento() == null)
            throw new Exception("Data de nascimento obrigatória.");
        if (cpf == null || cpf.trim().isEmpty())
            throw new Exception("CPF obrigatório.");

        cpf = cpf.replaceAll("[^0-9]", "");

        if (!consultarReceitaFederal(cpf, vo.getDataNascimento()))
            throw new Exception("ALERTA: CPF não corresponde à data de nascimento informada.");

        int idade = Period.between(vo.getDataNascimento(), LocalDate.now()).getYears();
        if (idade < 60)
            throw new Exception("O VIVER+ é exclusivo para pessoas com 60 anos ou mais.");

        if (dao.buscarPorEmail(vo.getEmail()) != null)
            throw new Exception("Este e-mail já está cadastrado. Tente fazer login.");

        return dao.salvar(vo);
    }

    // ── Login ─────────────────────────────────────────────────────────
    public UsuarioVO login(String email, String senha) throws Exception {
        UsuarioVO u = dao.buscarPorEmailESenha(email, senha);
        if (u == null) throw new Exception("E-mail ou senha incorretos.");
        return u;
    }

    // ── Postagem com Observer real ────────────────────────────────────
    public PostVO criarPostagem(UsuarioVO autor, String texto) throws Exception {
        if (texto == null || texto.trim().isEmpty())
            throw new Exception("A mensagem não pode estar vazia.");

        String min = texto.toLowerCase();
        if (min.contains("http://") || min.contains("https://") || min.contains("clique aqui"))
            throw new Exception("ALERTA DE SEGURANÇA: Links externos não são permitidos.");

        PostVO post = postDao.salvar(new PostVO(0, texto, autor));

        // ── OBSERVER ATIVO ──────────────────────────────────────────
        // Busca seguidores do autor no banco e notifica cada um deles
        List<UsuarioVO> seguidores = seguidorDao.listarSeguidores(autor.getId());
        for (UsuarioVO s : seguidores) {
            autor.adicionarSeguidor(new Seguidor(s));   // Seguidor salva no BD
        }
        if (!seguidores.isEmpty()) {
            autor.notificarSeguidores(
                autor.getNome() + " publicou: \"" + texto.substring(0, Math.min(texto.length(), 40)) + "...\""
            );
        }
        // ────────────────────────────────────────────────────────────

        return post;
    }

    // ── Curtidas ──────────────────────────────────────────────────────
    public boolean processarCurtida(int usuarioId, int postId) throws Exception {
        return postDao.alternarCurtida(usuarioId, postId);
    }

    // ── Comentários / Respostas ───────────────────────────────────────
    public void adicionarComentario(int usuarioId, int postId, String texto) throws Exception {
        if (texto == null || texto.trim().isEmpty())
            throw new Exception("O comentário não pode estar vazio.");

        postDao.salvarResposta(usuarioId, postId, texto);

        // Notifica o autor do post que recebeu um comentário
        try {
            UsuarioVO comentador = dao.buscarPorId(usuarioId);
            PostVO post = postDao.buscarPorId(postId);
            if (post != null && post.getAutor() != null && comentador != null) {
                int autorPostId = post.getAutor().getId();
                if (autorPostId != usuarioId) {   // Não notifica a si mesmo
                    notifDao.salvar(autorPostId,
                        comentador.getNome() + " comentou no seu momento: \"" +
                        texto.substring(0, Math.min(texto.length(), 40)) + "...\"");
                }
            }
        } catch (Exception e) {
            System.err.println("[BO] Erro ao notificar comentário: " + e.getMessage());
        }
    }

    // ── Seguir / Deixar de seguir ─────────────────────────────────────
    public String seguirOuDeixar(int seguidorId, int seguidoId) throws Exception {
        if (seguidorId == seguidoId)
            throw new Exception("Você não pode seguir a si mesmo.");

        UsuarioVO seguido = dao.buscarPorId(seguidoId);
        if (seguido == null) throw new Exception("Usuário não encontrado.");

        if (seguidorDao.jaSegue(seguidorId, seguidoId)) {
            seguidorDao.deixarDeSeguir(seguidorId, seguidoId);
            return "deixou";
        } else {
            seguidorDao.seguir(seguidorId, seguidoId);
            // Notifica o usuário seguido
            UsuarioVO seguidor = dao.buscarPorId(seguidorId);
            if (seguidor != null) {
                notifDao.salvar(seguidoId, seguidor.getNome() + " começou a seguir você! 🌱");
            }
            return "seguiu";
        }
    }

    // ── Foto de Perfil ────────────────────────────────────────────────
    public void atualizarFoto(int usuarioId, String base64) throws Exception {
        if (base64 == null || base64.isEmpty())
            throw new Exception("Foto inválida.");
        dao.atualizarFoto(usuarioId, base64);
    }
}