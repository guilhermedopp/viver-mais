import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.bo.UsuarioBO;
import com.dao.*;
import com.vo.*;

import io.javalin.Javalin;

public class Main {
    public static void main(String[] args) {

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
        }).start(8080);

        System.out.println("🔥 VIVER+ online em: http://localhost:8080");

        UsuarioBO        usuarioBO    = new UsuarioBO();
        PostDAO          postDAO      = new PostDAO();
        UsuarioDAO       usuarioDAO   = new UsuarioDAO();
        ComunidadeDAO    comDAO       = new ComunidadeDAO();
        NotificacaoDAO   notifDAO     = new NotificacaoDAO();
        MensagemDAO      msgDAO       = new MensagemDAO();
        MensagemGrupoDAO msgGrupoDAO  = new MensagemGrupoDAO();

        // ── AUTH ──────────────────────────────────────────────────────────
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
                ctx.status(201).json(usuarioBO.cadastrar(vo));
            } catch (Exception e) { ctx.status(400).result(e.getMessage()); }
        });

        // ── FEED ──────────────────────────────────────────────────────────
        app.get("/api/posts", ctx -> {
            try {
                int uid = 0;
                try { uid = Integer.parseInt(ctx.queryParam("usuarioId")); } catch (Exception ignored) {}
                boolean soSeguidos = "seguindo".equals(ctx.queryParam("filtro"));
                ctx.json(soSeguidos ? postDAO.listarDosSeguidos(uid) : postDAO.listarTodos(uid));
            } catch (Exception e) { ctx.status(500).result(e.getMessage()); }
        });

        app.post("/api/posts", ctx -> {
            try {
                DadosPost d = ctx.bodyAsClass(DadosPost.class);
                ctx.status(201).json(usuarioBO.criarPostagem(d.autor, d.texto, d.imagem));
            } catch (Exception e) { ctx.status(400).result(e.getMessage()); }
        });

        app.post("/api/posts/{id}/ver", ctx -> {
            try {
                int postId = Integer.parseInt(ctx.pathParam("id"));
                DadosInteracao d = ctx.bodyAsClass(DadosInteracao.class);
                postDAO.marcarComoVisto(d.usuarioId, postId);
                ctx.status(200).result("ok");
            } catch (Exception e) { ctx.status(400).result(e.getMessage()); }
        });

        app.post("/api/posts/{id}/curtir", ctx -> {
            try {
                int postId = Integer.parseInt(ctx.pathParam("id"));
                DadosInteracao d = ctx.bodyAsClass(DadosInteracao.class);
                ctx.result(usuarioBO.processarCurtida(d.usuarioId, postId) ? "curtiu" : "descurtiu");
            } catch (Exception e) { ctx.status(400).result(e.getMessage()); }
        });

        app.post("/api/posts/{id}/responder", ctx -> {
            try {
                int postId = Integer.parseInt(ctx.pathParam("id"));
                DadosInteracao d = ctx.bodyAsClass(DadosInteracao.class);
                usuarioBO.adicionarComentario(d.usuarioId, postId, d.texto);
                ctx.status(201).result("ok");
            } catch (Exception e) { ctx.status(400).result(e.getMessage()); }
        });

        // ── USUÁRIOS / PERFIL ─────────────────────────────────────────────
        app.get("/api/usuarios", ctx -> {
            try { ctx.json(usuarioDAO.listarTodos()); }
            catch (Exception e) { ctx.status(500).result(e.getMessage()); }
        });

        // ROTA NOVA: buscar usuário por e-mail (usado pelo convite de grupo)
        // ATENÇÃO: esta rota deve vir ANTES de /api/usuarios/{id}
        app.get("/api/usuarios/buscar", ctx -> {
            try {
                String email = ctx.queryParam("email");
                if (email == null || email.isEmpty()) {
                    ctx.status(400).result("E-mail não informado."); return;
                }
                UsuarioVO u = usuarioDAO.buscarPorEmail(email);
                if (u == null) { ctx.status(404).result("Nenhum usuário encontrado com esse e-mail."); return; }
                // Retorna apenas id e nome por segurança
                ctx.json(Map.of("id", u.getId(), "nome", u.getNome()));
            } catch (Exception e) { ctx.status(500).result(e.getMessage()); }
        });

        app.get("/api/usuarios/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                UsuarioVO u = usuarioDAO.buscarPorId(id);
                if (u == null) { ctx.status(404).result("Não encontrado."); return; }
                List<PostVO> posts = postDAO.listarPorUsuario(id);
                EstatisticasPerfilVO stats = usuarioDAO.buscarEstatisticas(id);
                ctx.json(Map.of("usuario", u, "posts", posts, "estatisticas", stats));
            } catch (Exception e) { ctx.status(500).result(e.getMessage()); }
        });

        app.get("/api/usuarios/{id}/estatisticas", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                ctx.json(usuarioDAO.buscarEstatisticas(id));
            } catch (Exception e) { ctx.status(500).result(e.getMessage()); }
        });

        app.get("/api/usuarios/{id}/sigo", ctx -> {
            try {
                int seguidoId  = Integer.parseInt(ctx.pathParam("id"));
                int seguidorId = Integer.parseInt(ctx.queryParam("usuarioId"));
                SeguidorDAO sd = new SeguidorDAO();
                ctx.json(Map.of("sigo", sd.jaSegue(seguidorId, seguidoId)));
            } catch (Exception e) { ctx.status(400).result(e.getMessage()); }
        });

        app.post("/api/usuarios/{id}/seguir", ctx -> {
            try {
                int seguidoId = Integer.parseInt(ctx.pathParam("id"));
                DadosInteracao d = ctx.bodyAsClass(DadosInteracao.class);
                ctx.result(usuarioBO.seguirOuDeixar(d.usuarioId, seguidoId));
            } catch (Exception e) { ctx.status(400).result(e.getMessage()); }
        });

        app.post("/api/usuarios/{id}/foto", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                DadosFoto d = ctx.bodyAsClass(DadosFoto.class);
                usuarioBO.atualizarFoto(id, d.base64);
                ctx.result("ok");
            } catch (Exception e) { ctx.status(400).result(e.getMessage()); }
        });

        // ── NOTIFICAÇÕES ──────────────────────────────────────────────────
        app.get("/api/notificacoes/{uid}", ctx -> {
            try { ctx.json(notifDAO.listarPorUsuario(Integer.parseInt(ctx.pathParam("uid")))); }
            catch (Exception e) { ctx.status(500).result(e.getMessage()); }
        });

        app.get("/api/notificacoes/{uid}/nao-lidas", ctx -> {
            try { ctx.json(Map.of("total", notifDAO.contarNaoLidas(Integer.parseInt(ctx.pathParam("uid"))))); }
            catch (Exception e) { ctx.status(500).result(e.getMessage()); }
        });

        app.post("/api/notificacoes/{uid}/ler", ctx -> {
            try { notifDAO.marcarTodasComoLidas(Integer.parseInt(ctx.pathParam("uid"))); ctx.result("ok"); }
            catch (Exception e) { ctx.status(500).result(e.getMessage()); }
        });

        // ── CHAT PRIVADO ──────────────────────────────────────────────────
        app.get("/api/chat/contatos/{uid}", ctx -> {
            try { ctx.json(msgDAO.listarContatos(Integer.parseInt(ctx.pathParam("uid")))); }
            catch (Exception e) { ctx.status(500).result(e.getMessage()); }
        });

        app.get("/api/chat/{uidA}/{uidB}", ctx -> {
            try {
                int a = Integer.parseInt(ctx.pathParam("uidA"));
                int b = Integer.parseInt(ctx.pathParam("uidB"));
                msgDAO.marcarComoLidas(b, a);
                ctx.json(msgDAO.buscarConversa(a, b));
            } catch (Exception e) { ctx.status(500).result(e.getMessage()); }
        });

        app.post("/api/chat", ctx -> {
            try {
                DadosMensagem d = ctx.bodyAsClass(DadosMensagem.class);
                msgDAO.enviar(d.remetenteId, d.destinatarioId, d.conteudo);
                ctx.status(201).result("ok");
            } catch (Exception e) { ctx.status(400).result(e.getMessage()); }
        });

        app.get("/api/chat/nao-lidas/{uid}", ctx -> {
            try { ctx.json(Map.of("total", msgDAO.contarNaoLidas(Integer.parseInt(ctx.pathParam("uid"))))); }
            catch (Exception e) { ctx.status(500).result(e.getMessage()); }
        });

        // ── GRUPOS ────────────────────────────────────────────────────────
        app.get("/api/comunidades", ctx -> {
            try {
                int uid = Integer.parseInt(ctx.queryParam("usuarioId"));
                ctx.json(comDAO.listarDoUsuario(uid));
            } catch (Exception e) { ctx.status(500).result(e.getMessage()); }
        });

        app.post("/api/comunidades", ctx -> {
            try {
                DadosNovaComunidade d = ctx.bodyAsClass(DadosNovaComunidade.class);
                if (d.nome == null || d.nome.trim().isEmpty())
                    throw new Exception("Nome do grupo é obrigatório.");
                ctx.status(201).json(comDAO.salvar(new ComunidadeVO(0, d.nome, d.descricao), d.criadorId));
            } catch (Exception e) { ctx.status(400).result(e.getMessage()); }
        });

        app.get("/api/comunidades/{id}/membros", ctx -> {
            try { ctx.json(comDAO.listarMembros(Integer.parseInt(ctx.pathParam("id")))); }
            catch (Exception e) { ctx.status(500).result(e.getMessage()); }
        });

        app.post("/api/comunidades/{id}/convidar", ctx -> {
            try {
                int comunidadeId = Integer.parseInt(ctx.pathParam("id"));
                DadosConvite d = ctx.bodyAsClass(DadosConvite.class);
                if (!comDAO.ehMembro(comunidadeId, d.convidanteId))
                    throw new Exception("Você não é membro deste grupo.");
                int conviteId = comDAO.convidar(comunidadeId, d.convidanteId, d.convidadoId);
                UsuarioVO convidante = usuarioDAO.buscarPorId(d.convidanteId);
                ComunidadeVO grupo   = comDAO.buscarPorId(comunidadeId);
                if (convidante != null && grupo != null)
                    notifDAO.salvar(d.convidadoId,
                        convidante.getNome() + " te convidou para o grupo \"" + grupo.getNome() + "\" 💬");
                ctx.status(201).json(Map.of("conviteId", conviteId));
            } catch (Exception e) { ctx.status(400).result(e.getMessage()); }
        });

        app.post("/api/convites/{id}/responder", ctx -> {
            try {
                int conviteId = Integer.parseInt(ctx.pathParam("id"));
                DadosRespostaConvite d = ctx.bodyAsClass(DadosRespostaConvite.class);
                comDAO.responderConvite(conviteId, d.usuarioId, d.aceitar);
                ctx.result(d.aceitar ? "aceito" : "recusado");
            } catch (Exception e) { ctx.status(400).result(e.getMessage()); }
        });

        app.get("/api/convites/pendentes/{uid}", ctx -> {
            try { ctx.json(comDAO.listarConvitesPendentes(Integer.parseInt(ctx.pathParam("uid")))); }
            catch (Exception e) { ctx.status(500).result(e.getMessage()); }
        });

        app.get("/api/comunidades/{id}/mensagens", ctx -> {
            try {
                int comunidadeId = Integer.parseInt(ctx.pathParam("id"));
                int uid = Integer.parseInt(ctx.queryParam("usuarioId"));
                if (!comDAO.ehMembro(comunidadeId, uid))
                    throw new Exception("Acesso negado: você não é membro deste grupo.");
                ctx.json(msgGrupoDAO.listarPorGrupo(comunidadeId));
            } catch (Exception e) { ctx.status(403).result(e.getMessage()); }
        });

        app.post("/api/comunidades/{id}/mensagens", ctx -> {
            try {
                int comunidadeId = Integer.parseInt(ctx.pathParam("id"));
                DadosMensagemGrupo d = ctx.bodyAsClass(DadosMensagemGrupo.class);
                if (!comDAO.ehMembro(comunidadeId, d.usuarioId))
                    throw new Exception("Acesso negado: você não é membro deste grupo.");
                msgGrupoDAO.enviar(comunidadeId, d.usuarioId, d.conteudo);
                ctx.status(201).result("ok");
            } catch (Exception e) { ctx.status(403).result(e.getMessage()); }
        });

        System.out.println("✅ Todas as rotas configuradas!");
    }

    // ── DTOs ──────────────────────────────────────────────────────────────
    public static class DadosLogin           { public String email, senha;                                               public DadosLogin() {} }
    public static class DadosCadastro        { public String nome, email, senha, dataNascimento;                         public DadosCadastro() {} }
    public static class DadosInteracao       { public int usuarioId; public String texto;                                public DadosInteracao() {} }
    public static class DadosPost            { public String texto, imagem, destinoTipo; public int destinoId; public UsuarioVO autor; public DadosPost() {} }
    public static class DadosNovaComunidade  { public String nome, descricao; public int criadorId;                     public DadosNovaComunidade() {} }
    public static class DadosFoto            { public String base64;                                                     public DadosFoto() {} }
    public static class DadosMensagem        { public int remetenteId, destinatarioId; public String conteudo;           public DadosMensagem() {} }
    public static class DadosMensagemGrupo   { public int usuarioId; public String conteudo;                            public DadosMensagemGrupo() {} }
    public static class DadosConvite         { public int convidanteId, convidadoId;                                    public DadosConvite() {} }
    public static class DadosRespostaConvite { public int usuarioId; public boolean aceitar;                             public DadosRespostaConvite() {} }
}