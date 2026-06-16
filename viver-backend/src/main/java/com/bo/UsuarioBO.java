package com.bo;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
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

    // ── Cadastro (Sem CPF, com validação de e-mail Regex) ─────────────
    public UsuarioVO cadastrar(UsuarioVO vo) throws Exception {
        // Validação de formato de e-mail (Regex)
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        if (vo.getEmail() == null || !vo.getEmail().matches(emailRegex))
            throw new Exception("O e-mail fornecido não é válido.");

        if (vo.getNome() == null || vo.getNome().trim().isEmpty())
            throw new Exception("O nome não pode estar vazio.");
        if (vo.getSenha() == null || vo.getSenha().length() < 3)
            throw new Exception("Senha muito curta. Mínimo 3 caracteres.");
        if (vo.getDataNascimento() == null)
            throw new Exception("Data de nascimento obrigatória.");

        int idade = Period.between(vo.getDataNascimento(), LocalDate.now()).getYears();
        if (idade < 60)
            throw new Exception("O VIVER+ é exclusivo para pessoas com 60 anos ou mais.");

        if (dao.buscarPorEmail(vo.getEmail()) != null)
            throw new Exception("Este e-mail já está registado.");

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
        List<UsuarioVO> seguidores = seguidorDao.listarSeguidores(autor.getId());
        for (UsuarioVO s : seguidores) {
            autor.adicionarSeguidor(new Seguidor(s));
        }
        if (!seguidores.isEmpty()) {
            autor.notificarSeguidores(
                autor.getNome() + " publicou: \"" + texto.substring(0, Math.min(texto.length(), 40)) + "...\""
            );
        }
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

        try {
            UsuarioVO comentador = dao.buscarPorId(usuarioId);
            PostVO post = postDao.buscarPorId(postId);
            if (post != null && post.getAutor() != null && comentador != null) {
                int autorPostId = post.getAutor().getId();
                if (autorPostId != usuarioId) {
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