import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.bo.UsuarioBO;
import com.dao.ComunidadeDAO;
import com.dao.NotificacaoDAO;
import com.dao.PostDAO;
import com.dao.UsuarioDAO;
import com.vo.ComunidadeVO;
import com.vo.PostVO;
import com.vo.UsuarioVO;

import io.javalin.Javalin;

public class Main {
    public static void main(String[] args) {

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
        }).start(8080);

        System.out.println("🔥 VIVER+ online em: http://localhost:8080");

        UsuarioBO    usuarioBO    = new UsuarioBO();
        PostDAO      postDAO      = new PostDAO();
        UsuarioDAO   usuarioDAO   = new UsuarioDAO();
        ComunidadeDAO comDAO      = new ComunidadeDAO();
        NotificacaoDAO notifDAO   = new NotificacaoDAO();

        // ── AUTENTICAÇÃO ──────────────────────────────────────────────────
        app.post("/api/login", ctx -> {
            try {
                DadosLogin d = ctx.bodyAsClass(DadosLogin.class);
                ctx.json(usuarioBO.login(d.email, d.senha));
            } catch (Exception e) { ctx.status(401).result(e.getMessage()); }
        });

        app.post("/api/cadastro", ctx -> {
            try {
                DadosCadastro d = ctx.bodyAsClass(DadosCadastro.class);
                UsuarioVO vo = new UsuarioVO(0, d.nome, d.email, d.senha, LocalDate.parse(d.dataNascimento));
                ctx.status(201).json(usuarioBO.cadastrar(vo, d.cpf));
            } catch (Exception e) { ctx.status(400).result(e.getMessage()); }
        });

        // ── FEED DE POSTAGENS ─────────────────────────────────────────────
        app.get("/api/posts", ctx -> {
            try {
                // Aceita ?usuarioId=X para marcar vistos/não vistos
                int uid = 0;
                try { uid = Integer.parseInt(ctx.queryParam("usuarioId")); } catch (Exception ignored) {}
                ctx.json(postDAO.listarTodos(uid));
            } catch (Exception e) { ctx.status(500).result(e.getMessage()); }
        });

        app.post("/api/posts", ctx -> {
            try {
                DadosPost d = ctx.bodyAsClass(DadosPost.class);
                PostVO post = new PostVO(0, d.texto, d.autor);
                if ("COMUNIDADE".equalsIgnoreCase(d.destinoTipo)) {
                    ComunidadeVO c = new ComunidadeVO(); c.setId(d.destinoId);
                    post.setDestino(c); post.setDestinoTipo("COMUNIDADE");
                }
                ctx.status(201).json(usuarioBO.criarPostagem(d.autor, d.texto));
            } catch (Exception e) { ctx.status(400).result(e.getMessage()); }
        });

        // Marcar post como visto
        app.post("/api/posts/{id}/ver", ctx -> {
            try {
                int postId = Integer.parseInt(ctx.pathParam("id"));
                DadosInteracao d = ctx.bodyAsClass(DadosInteracao.class);
                postDAO.marcarComoVisto(d.usuarioId, postId);
                ctx.status(200).result("ok");
            } catch (Exception e) { ctx.status(400).result(e.getMessage()); }
        });

        // ── INTERAÇÕES (curtir / responder) ───────────────────────────────
        app.post("/api/posts/{id}/curtir", ctx -> {
            try {
                int postId = Integer.parseInt(ctx.pathParam("id"));
                DadosInteracao d = ctx.bodyAsClass(DadosInteracao.class);
                boolean curtiu = usuarioBO.processarCurtida(d.usuarioId, postId);
                ctx.result(curtiu ? "curtiu" : "descurtiu");
            } catch (Exception e) { ctx.status(400).result(e.getMessage()); }
        });

        app.post("/api/posts/{id}/responder", ctx -> {
            try {
                int postId = Integer.parseInt(ctx.pathParam("id"));
                DadosInteracao d = ctx.bodyAsClass(DadosInteracao.class);
                usuarioBO.adicionarComentario(d.usuarioId, postId, d.texto);
                ctx.status(201).result("Comentário gravado!");
            } catch (Exception e) { ctx.status(400).result(e.getMessage()); }
        });

        // ── PERFIL ────────────────────────────────────────────────────────
        // Perfil público de qualquer usuário
        app.get("/api/usuarios/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                UsuarioVO u = usuarioDAO.buscarPorId(id);
                if (u == null) { ctx.status(404).result("Usuário não encontrado."); return; }
                // Adiciona os posts do usuário no retorno
                List<PostVO> posts = postDAO.listarPorUsuario(id);
                ctx.json(Map.of("usuario", u, "posts", posts));
            } catch (Exception e) { ctx.status(500).result(e.getMessage()); }
        });

        // Lista todos os usuários (aba de pessoas)
        app.get("/api/usuarios", ctx -> {
            try { ctx.json(usuarioDAO.listarTodos()); }
            catch (Exception e) { ctx.status(500).result(e.getMessage()); }
        });

        // Seguir / deixar de seguir
        app.post("/api/usuarios/{id}/seguir", ctx -> {
            try {
                int seguidoId = Integer.parseInt(ctx.pathParam("id"));
                DadosInteracao d = ctx.bodyAsClass(DadosInteracao.class);
                String resultado = usuarioBO.seguirOuDeixar(d.usuarioId, seguidoId);
                ctx.result(resultado);
            } catch (Exception e) { ctx.status(400).result(e.getMessage()); }
        });

        // Upload de foto de perfil (base64)
        app.post("/api/usuarios/{id}/foto", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                DadosFoto d = ctx.bodyAsClass(DadosFoto.class);
                usuarioBO.atualizarFoto(id, d.base64);
                ctx.result("Foto atualizada!");
            } catch (Exception e) { ctx.status(400).result(e.getMessage()); }
        });

        // ── NOTIFICAÇÕES (Observer) ───────────────────────────────────────
        app.get("/api/notificacoes/{usuarioId}", ctx -> {
            try {
                int uid = Integer.parseInt(ctx.pathParam("usuarioId"));
                ctx.json(notifDAO.listarPorUsuario(uid));
            } catch (Exception e) { ctx.status(500).result(e.getMessage()); }
        });

        app.get("/api/notificacoes/{usuarioId}/nao-lidas", ctx -> {
            try {
                int uid = Integer.parseInt(ctx.pathParam("usuarioId"));
                ctx.json(Map.of("total", notifDAO.contarNaoLidas(uid)));
            } catch (Exception e) { ctx.status(500).result(e.getMessage()); }
        });

        app.post("/api/notificacoes/{usuarioId}/ler", ctx -> {
            try {
                int uid = Integer.parseInt(ctx.pathParam("usuarioId"));
                notifDAO.marcarTodasComoLidas(uid);
                ctx.result("ok");
            } catch (Exception e) { ctx.status(500).result(e.getMessage()); }
        });

        // ── COMUNIDADES ───────────────────────────────────────────────────
        app.get("/api/comunidades", ctx -> {
            try { ctx.json(comDAO.listarTodas()); }
            catch (Exception e) { ctx.status(500).result(e.getMessage()); }
        });

        app.post("/api/comunidades", ctx -> {
            try {
                DadosNovaComunidade d = ctx.bodyAsClass(DadosNovaComunidade.class);
                if (d.nome == null || d.nome.trim().isEmpty())
                    throw new Exception("Nome da comunidade é obrigatório.");
                ctx.status(201).json(comDAO.salvar(new ComunidadeVO(0, d.nome, d.descricao)));
            } catch (Exception e) { ctx.status(400).result(e.getMessage()); }
        });
    }

    // ── DTOs ──────────────────────────────────────────────────────────────
    public static class DadosLogin          { public String email, senha; public DadosLogin() {} }
    public static class DadosCadastro       { public String nome, email, senha, dataNascimento, cpf; public DadosCadastro() {} }
    public static class DadosInteracao      { public int usuarioId; public String texto; public DadosInteracao() {} }
    public static class DadosPost           { public String texto, destinoTipo; public int destinoId; public UsuarioVO autor; public DadosPost() {} }
    public static class DadosNovaComunidade { public String nome, descricao; public DadosNovaComunidade() {} }
    public static class DadosFoto           { public String base64; public DadosFoto() {} }
}