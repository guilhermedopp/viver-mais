import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.bo.UsuarioBO;
import com.dao.*;
import com.vo.*;
import io.javalin.Javalin;

public class Main {
    // ID do cliente do Google Cloud Console (substitua pelo seu)
    private static final String GOOGLE_CLIENT_ID = "1095979412262-me3kh924htbgh5fmt434hqpkhjsv715s.apps.googleusercontent.com";

    public static void main(String[] args) {

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
            // CORREÇÃO DO BUG DE FOTO: aumenta o limite para 20 MB
            config.http.maxRequestSize = 20_000_000L;
        }).start(8080);

        System.out.println("🔥 VIVER+ online em: http://localhost:8080");

        UsuarioBO        usuarioBO   = new UsuarioBO();
        PostDAO          postDAO     = new PostDAO();
        UsuarioDAO       usuarioDAO  = new UsuarioDAO();
        ComunidadeDAO    comDAO      = new ComunidadeDAO();
        NotificacaoDAO   notifDAO    = new NotificacaoDAO();
        MensagemDAO      msgDAO      = new MensagemDAO();
        MensagemGrupoDAO msgGrupoDAO = new MensagemGrupoDAO();

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
                UsuarioVO vo = new UsuarioVO(0, d.nome, d.nickname, d.email, d.senha,
                        d.dataNascimento != null ? LocalDate.parse(d.dataNascimento) : null);
                ctx.status(201).json(usuarioBO.cadastrar(vo));
            } catch (Exception e) { ctx.status(400).result(e.getMessage()); }
        });

        // ── GOOGLE OAUTH ──────────────────────────────────────────────────
        // Frontend envia o credential (JWT) recebido do Google
        app.post("/api/auth/google", ctx -> {
            try {
                DadosGoogle d = ctx.bodyAsClass(DadosGoogle.class);
                // Verifica o token com a API do Google
                URL url = new URL("https://oauth2.googleapis.com/tokeninfo?id_token=" + d.credential);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                if (conn.getResponseCode() != 200)
                    throw new Exception("Token Google inválido.");
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }
                String json = sb.toString();
                // Extrai campos do JSON da resposta do Google (parsing simples)
                String googleId  = extrairCampo(json, "sub");
                String email     = extrairCampo(json, "email");
                String nome      = extrairCampo(json, "name");
                String foto      = extrairCampo(json, "picture");
                // Opcional: verifica se o audience (aud) bate com o nosso client_id
                String aud = extrairCampo(json, "aud");
                if (!GOOGLE_CLIENT_ID.equals("SEU_GOOGLE_CLIENT_ID_AQUI.apps.googleusercontent.com")
                        && !GOOGLE_CLIENT_ID.equals(aud))
                    throw new Exception("Token não pertence a esta aplicação.");

                UsuarioVO usuario = usuarioBO.loginOuCadastrarGoogle(googleId, nome, email, foto);
                ctx.json(usuario);
            } catch (Exception e) { ctx.status(401).result(e.getMessage()); }
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

        // ── USUÁRIOS ──────────────────────────────────────────────────────
        app.get("/api/usuarios", ctx -> {
            try { ctx.json(usuarioDAO.listarTodos()); }
            catch (Exception e) { ctx.status(500).result(e.getMessage()); }
        });

        // Buscar por e-mail OU @nickname — ANTES de /api/usuarios/{id}
        app.get("/api/usuarios/buscar", ctx -> {
            try {
                String email    = ctx.queryParam("email");
                String nickname = ctx.queryParam("nickname");
                UsuarioVO u = null;
                if (email != null && !email.isEmpty())
                    u = usuarioDAO.buscarPorEmail(email);
                else if (nickname != null && !nickname.isEmpty())
                    u = usuarioDAO.buscarPorNickname(nickname.replace("@", ""));
                if (u == null) { ctx.status(404).result("Usuário não encontrado."); return; }
                ctx.json(Map.of("id", u.getId(), "nome", u.getNome(),
                                "nickname", u.getNickname() != null ? u.getNickname() : ""));
            } catch (Exception e) { ctx.status(500).result(e.getMessage()); }
        });

        // Verificar disponibilidade de @nickname
        app.get("/api/usuarios/nickname/{nick}/disponivel", ctx -> {
            try {
                String nick = ctx.pathParam("nick").replace("@", "");
                int excluirId = 0;
                try { excluirId = Integer.parseInt(ctx.queryParam("excluirId")); } catch (Exception ignored) {}
                boolean disponivel = usuarioDAO.nicknameDisponivel(nick, excluirId);
                ctx.json(Map.of("disponivel", disponivel));
            } catch (Exception e) { ctx.status(500).result(e.getMessage()); }
        });

        app.get("/api/usuarios/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                UsuarioVO u = usuarioDAO.buscarPorId(id);
                if (u == null) { ctx.status(404).result("Não encontrado."); return; }
                ctx.json(Map.of("usuario", u, "posts", postDAO.listarPorUsuario(id),
                                "estatisticas", usuarioDAO.buscarEstatisticas(id)));
            } catch (Exception e) { ctx.status(500).result(e.getMessage()); }
        });

        app.get("/api/usuarios/{id}/estatisticas", ctx -> {
            try { ctx.json(usuarioDAO.buscarEstatisticas(Integer.parseInt(ctx.pathParam("id")))); }
            catch (Exception e) { ctx.status(500).result(e.getMessage()); }
        });

        app.get("/api/usuarios/{id}/sigo", ctx -> {
            try {
                int seguidoId  = Integer.parseInt(ctx.pathParam("id"));
                int seguidorId = Integer.parseInt(ctx.queryParam("usuarioId"));
                ctx.json(Map.of("sigo", new SeguidorDAO().jaSegue(seguidorId, seguidoId)));
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

        // Atualizar @nickname
        app.put("/api/usuarios/{id}/nickname", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                DadosNickname d = ctx.bodyAsClass(DadosNickname.class);
                usuarioBO.atualizarNickname(id, d.nickname.replace("@", ""));
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

        // ── CHAT ──────────────────────────────────────────────────────────
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
            try { ctx.json(comDAO.listarDoUsuario(Integer.parseInt(ctx.queryParam("usuarioId")))); }
            catch (Exception e) { ctx.status(500).result(e.getMessage()); }
        });

        app.post("/api/comunidades", ctx -> {
            try {
                DadosNovaComunidade d = ctx.bodyAsClass(DadosNovaComunidade.class);
                if (d.nome == null || d.nome.trim().isEmpty())
                    throw new Exception("Nome do grupo é obrigatório.");
                ctx.status(201).json(comDAO.salvar(new ComunidadeVO(0, d.nome, d.descricao), d.criadorId));
            } catch (Exception e) { ctx.status(400).result(e.getMessage()); }
        });

        // Editar grupo (nome, descrição, foto) — só ADMIN
        app.put("/api/comunidades/{id}", ctx -> {
            try {
                int comunidadeId = Integer.parseInt(ctx.pathParam("id"));
                DadosEditarGrupo d = ctx.bodyAsClass(DadosEditarGrupo.class);
                if (!comDAO.ehAdmin(comunidadeId, d.usuarioId))
                    throw new Exception("Apenas o administrador pode editar o grupo.");
                comDAO.atualizar(comunidadeId, d.nome, d.descricao, d.fotoGrupo);
                ctx.result("ok");
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
                    throw new Exception("Acesso negado.");
                ctx.json(msgGrupoDAO.listarPorGrupo(comunidadeId));
            } catch (Exception e) { ctx.status(403).result(e.getMessage()); }
        });

        app.post("/api/comunidades/{id}/mensagens", ctx -> {
            try {
                int comunidadeId = Integer.parseInt(ctx.pathParam("id"));
                DadosMensagemGrupo d = ctx.bodyAsClass(DadosMensagemGrupo.class);
                if (!comDAO.ehMembro(comunidadeId, d.usuarioId))
                    throw new Exception("Acesso negado.");
                msgGrupoDAO.enviar(comunidadeId, d.usuarioId, d.conteudo);
                ctx.status(201).result("ok");
            } catch (Exception e) { ctx.status(403).result(e.getMessage()); }
        });

        System.out.println("✅ Todas as rotas configuradas!");
    }

    // Extrai campo de um JSON simples (sem biblioteca)
    private static String extrairCampo(String json, String campo) {
        String chave = "\"" + campo + "\":\"";
        int inicio = json.indexOf(chave);
        if (inicio == -1) return "";
        inicio += chave.length();
        int fim = json.indexOf("\"", inicio);
        return fim == -1 ? "" : json.substring(inicio, fim);
    }

    // ── DTOs ──────────────────────────────────────────────────────────────
    public static class DadosLogin           { public String email, senha;                                                                   public DadosLogin() {} }
    public static class DadosCadastro        { public String nome, nickname, email, senha, dataNascimento;                                    public DadosCadastro() {} }
    public static class DadosGoogle          { public String credential;                                                                     public DadosGoogle() {} }
    public static class DadosInteracao       { public int usuarioId; public String texto;                                                    public DadosInteracao() {} }
    public static class DadosPost            { public String texto, imagem, destinoTipo; public int destinoId; public UsuarioVO autor;        public DadosPost() {} }
    public static class DadosNovaComunidade  { public String nome, descricao; public int criadorId;                                         public DadosNovaComunidade() {} }
    public static class DadosEditarGrupo     { public int usuarioId; public String nome, descricao, fotoGrupo;                               public DadosEditarGrupo() {} }
    public static class DadosFoto            { public String base64;                                                                         public DadosFoto() {} }
    public static class DadosMensagem        { public int remetenteId, destinatarioId; public String conteudo;                               public DadosMensagem() {} }
    public static class DadosMensagemGrupo   { public int usuarioId; public String conteudo;                                                public DadosMensagemGrupo() {} }
    public static class DadosConvite         { public int convidanteId, convidadoId;                                                        public DadosConvite() {} }
    public static class DadosRespostaConvite { public int usuarioId; public boolean aceitar;                                                 public DadosRespostaConvite() {} }
    public static class DadosNickname        { public String nickname;                                                                       public DadosNickname() {} }
}