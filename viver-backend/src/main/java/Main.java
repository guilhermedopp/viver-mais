import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.javalin.websocket.WsContext;

import com.bo.UsuarioBO;
import com.dao.*;
import com.vo.*;
import io.javalin.Javalin;

public class Main {
    private static final String GOOGLE_CLIENT_ID = "1095979412262-me3kh924htbgh5fmt434hqpkhjsv715s.apps.googleusercontent.com";
    private static final String JWT_SECRET = "VIVER_MAIS_SEGREDO_SUPER_SEGURO";
    
    // Mapa para gerir conexões ativas do WebSocket
    public static Map<Integer, WsContext> sessoesWS = new ConcurrentHashMap<>();

    public static void main(String[] args) {

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
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

        // ── WEBSOCKETS (Observer em tempo real) ───────────────────────────
        app.ws("/ws/notificacoes/{uid}", ws -> {
            ws.onConnect(ctx -> {
                int uid = Integer.parseInt(ctx.pathParam("uid"));
                sessoesWS.put(uid, ctx);
            });
            ws.onClose(ctx -> {
                int uid = Integer.parseInt(ctx.pathParam("uid"));
                sessoesWS.remove(uid);
            });
        });

        // ── FILTRO JWT ────────────────────────────────────────────────────
        app.before("/api/*", ctx -> {
            String path = ctx.path();
            if (path.equals("/api/login") || path.equals("/api/cadastro") 
                || path.equals("/api/auth/google") || path.equals("/api/auth/completar-perfil") 
                || path.contains("/disponivel")) return;
            
            String authHeader = ctx.header("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ctx.status(401).result("Acesso Negado: Token JWT ausente.");
                return;
            }
            try {
                String token = authHeader.replace("Bearer ", "");
                JWT.require(Algorithm.HMAC256(JWT_SECRET)).build().verify(token);
            } catch (Exception e) {
                ctx.status(401).result("Token JWT inválido ou expirado.");
            }
        });

        // ── AUTH ──────────────────────────────────────────────────────────
        app.post("/api/login", ctx -> {
            try {
                DadosLogin d = ctx.bodyAsClass(DadosLogin.class);
                UsuarioVO u = usuarioBO.login(d.email, d.senha);
                String token = gerarToken(u);
                ctx.json(Map.of("token", token, "usuario", u));
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
        app.post("/api/auth/google", ctx -> {
            try {
                DadosGoogle d = ctx.bodyAsClass(DadosGoogle.class);
                URL url = new URL("https://oauth2.googleapis.com/tokeninfo?id_token=" + d.credential);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                if (conn.getResponseCode() != 200)
                    throw new Exception("Token Google inválido.");
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line; while ((line = br.readLine()) != null) sb.append(line);
                }
                String json = sb.toString();
                String googleId = extrairCampo(json, "sub");
                String email    = extrairCampo(json, "email");
                String nome     = extrairCampo(json, "name");
                String foto     = extrairCampo(json, "picture");
                
                UsuarioVO usuario = usuarioBO.loginOuCadastrarGoogle(googleId, nome, email, foto);
                String token = gerarToken(usuario);
                boolean precisaCompletar = usuario.getNickname() == null || usuario.getDataNascimento() == null;
                
                ctx.json(Map.of("token", token, "usuario", usuario, "precisaCompletar", precisaCompletar));
            } catch (Exception e) { ctx.status(401).result(e.getMessage()); }
        });

        app.post("/api/auth/completar-perfil", ctx -> {
            try {
                DadosCompletarPerfil d = ctx.bodyAsClass(DadosCompletarPerfil.class);
                usuarioBO.completarPerfilGoogle(d.usuarioId,
                        d.nickname != null ? d.nickname.replace("@", "") : null,
                        d.dataNascimento != null ? LocalDate.parse(d.dataNascimento) : null);
                UsuarioVO atualizado = usuarioDAO.buscarPorId(d.usuarioId);
                ctx.json(atualizado);
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
                postDAO.marcarComoVisto(ctx.bodyAsClass(DadosInteracao.class).usuarioId, Integer.parseInt(ctx.pathParam("id")));
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

        app.get("/api/usuarios/buscar", ctx -> {
            try {
                String email    = ctx.queryParam("email");
                String nickname = ctx.queryParam("nickname");
                UsuarioVO u = null;
                if (email != null && !email.isEmpty()) u = usuarioDAO.buscarPorEmail(email);
                else if (nickname != null && !nickname.isEmpty()) u = usuarioDAO.buscarPorNickname(nickname.replace("@", ""));
                if (u == null) { ctx.status(404).result("Usuário não encontrado."); return; }
                ctx.json(Map.of("id", u.getId(), "nome", u.getNome(), "nickname", u.getNickname() != null ? u.getNickname() : ""));
            } catch (Exception e) { ctx.status(500).result(e.getMessage()); }
        });

        app.get("/api/usuarios/nickname/{nick}/disponivel", ctx -> {
            try {
                int excluirId = 0;
                try { excluirId = Integer.parseInt(ctx.queryParam("excluirId")); } catch (Exception ignored) {}
                ctx.json(Map.of("disponivel", usuarioDAO.nicknameDisponivel(ctx.pathParam("nick").replace("@", ""), excluirId)));
            } catch (Exception e) { ctx.status(500).result(e.getMessage()); }
        });

        app.get("/api/usuarios/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                UsuarioVO u = usuarioDAO.buscarPorId(id);
                if (u == null) { ctx.status(404).result("Não encontrado."); return; }
                ctx.json(Map.of("usuario", u, "posts", postDAO.listarPorUsuario(id), "estatisticas", usuarioDAO.buscarEstatisticas(id)));
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
            } catch (UsuarioBO.AutoSeguirException e) {
                // Tratamento específico de erro HTTP 409
                ctx.status(409).result(e.getMessage());
            } catch (Exception e) { ctx.status(400).result(e.getMessage()); }
        });

        app.post("/api/usuarios/{id}/foto", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                usuarioBO.atualizarFoto(id, ctx.bodyAsClass(DadosFoto.class).base64);
                ctx.result("ok");
            } catch (Exception e) { ctx.status(400).result(e.getMessage()); }
        });

        app.put("/api/usuarios/{id}/nickname", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                usuarioBO.atualizarNickname(id, ctx.bodyAsClass(DadosNickname.class).nickname.replace("@", ""));
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
                int a = Integer.parseInt(ctx.pathParam("uidA")), b = Integer.parseInt(ctx.pathParam("uidB"));
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
                if (d.nome == null || d.nome.trim().isEmpty()) throw new Exception("Nome obrigatório.");
                ctx.status(201).json(comDAO.salvar(new ComunidadeVO(0, d.nome, d.descricao), d.criadorId));
            } catch (Exception e) { ctx.status(400).result(e.getMessage()); }
        });
        app.put("/api/comunidades/{id}", ctx -> {
            try {
                int comunidadeId = Integer.parseInt(ctx.pathParam("id"));
                DadosEditarGrupo d = ctx.bodyAsClass(DadosEditarGrupo.class);
                if (!comDAO.ehAdmin(comunidadeId, d.usuarioId)) throw new Exception("Apenas o administrador pode editar.");
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
                if (!comDAO.ehMembro(comunidadeId, d.convidanteId)) throw new Exception("Você não é membro.");
                int conviteId = comDAO.convidar(comunidadeId, d.convidanteId, d.convidadoId);
                UsuarioVO convidante = usuarioDAO.buscarPorId(d.convidanteId);
                ComunidadeVO grupo = comDAO.buscarPorId(comunidadeId);
                if (convidante != null && grupo != null)
                    notifDAO.salvar(d.convidadoId, convidante.getNome() + " te convidou para \"" + grupo.getNome() + "\" 💬");
                ctx.status(201).json(Map.of("conviteId", conviteId));
            } catch (Exception e) { ctx.status(400).result(e.getMessage()); }
        });
        app.post("/api/convites/{id}/responder", ctx -> {
            try {
                DadosRespostaConvite d = ctx.bodyAsClass(DadosRespostaConvite.class);
                comDAO.responderConvite(Integer.parseInt(ctx.pathParam("id")), d.usuarioId, d.aceitar);
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
                if (!comDAO.ehMembro(comunidadeId, uid)) throw new Exception("Acesso negado.");
                ctx.json(msgGrupoDAO.listarPorGrupo(comunidadeId));
            } catch (Exception e) { ctx.status(403).result(e.getMessage()); }
        });
        app.post("/api/comunidades/{id}/mensagens", ctx -> {
            try {
                int comunidadeId = Integer.parseInt(ctx.pathParam("id"));
                DadosMensagemGrupo d = ctx.bodyAsClass(DadosMensagemGrupo.class);
                if (!comDAO.ehMembro(comunidadeId, d.usuarioId)) throw new Exception("Acesso negado.");
                msgGrupoDAO.enviar(comunidadeId, d.usuarioId, d.conteudo);
                ctx.status(201).result("ok");
            } catch (Exception e) { ctx.status(403).result(e.getMessage()); }
        });

        System.out.println("✅ Todas as rotas configuradas!");
    }

    private static String extrairCampo(String json, String campo) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"" + campo + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        return m.find() ? m.group(1) : "";
    }

    // Método Auxiliar para Gerar Token JWT
    private static String gerarToken(UsuarioVO u) {
        return JWT.create()
            .withSubject(String.valueOf(u.getId()))
            .withClaim("email", u.getEmail())
            .withExpiresAt(new java.util.Date(System.currentTimeMillis() + 86400000)) // 24h
            .sign(Algorithm.HMAC256(JWT_SECRET));
    }

    // DTOs
    public static class DadosLogin           { public String email, senha;                                                                      public DadosLogin() {} }
    public static class DadosCadastro        { public String nome, nickname, email, senha, dataNascimento;                                      public DadosCadastro() {} }
    public static class DadosGoogle          { public String credential;                                                                        public DadosGoogle() {} }
    public static class DadosCompletarPerfil { public int usuarioId; public String nickname, dataNascimento;                                    public DadosCompletarPerfil() {} }
    public static class DadosInteracao       { public int usuarioId; public String texto;                                                       public DadosInteracao() {} }
    public static class DadosPost            { public String texto, imagem, destinoTipo; public int destinoId; public UsuarioVO autor;        public DadosPost() {} }
    public static class DadosNovaComunidade  { public String nome, descricao; public int criadorId;                                             public DadosNovaComunidade() {} }
    public static class DadosEditarGrupo     { public int usuarioId; public String nome, descricao, fotoGrupo;                                  public DadosEditarGrupo() {} }
    public static class DadosFoto            { public String base64;                                                                            public DadosFoto() {} }
    public static class DadosMensagem        { public int remetenteId, destinatarioId; public String conteudo;                                  public DadosMensagem() {} }
    public static class DadosMensagemGrupo   { public int usuarioId; public String conteudo;                                                    public DadosMensagemGrupo() {} }
    public static class DadosConvite         { public int convidanteId, convidadoId;                                                            public DadosConvite() {} }
    public static class DadosRespostaConvite { public int usuarioId; public boolean aceitar;                                                    public DadosRespostaConvite() {} }
    public static class DadosNickname        { public String nickname;                                                                          public DadosNickname() {} }
}