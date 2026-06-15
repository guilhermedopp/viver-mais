import java.time.LocalDate;

import com.bo.UsuarioBO;
import com.dao.ComunidadeDAO;
import com.dao.PostDAO;
import com.vo.ComunidadeVO;
import com.vo.PostVO;
import com.vo.UsuarioVO;

import io.javalin.Javalin; 

public class Main {
    public static void main(String[] args) {
        
        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> it.anyHost());
            });
        }).start(8080);

        System.out.println("🔥 Servidor Backend VIVER+ online! API rodando em: http://localhost:8080");

        UsuarioBO usuarioBO = new UsuarioBO();
        PostDAO postDAO = new PostDAO();
        ComunidadeDAO comunidadeDAO = new ComunidadeDAO();

        // Autenticação e Cadastro
        app.post("/api/login", ctx -> {
            try {
                DadosLogin dados = ctx.bodyAsClass(DadosLogin.class);
                UsuarioVO usuarioLogado = usuarioBO.login(dados.email, dados.senha);
                ctx.status(200).json(usuarioLogado);
            } catch (Exception e) { ctx.status(401).result(e.getMessage()); }
        });

        app.post("/api/cadastro", ctx -> {
            try {
                DadosCadastro dados = ctx.bodyAsClass(DadosCadastro.class);
                UsuarioVO novoUsuario = new UsuarioVO(0, dados.nome, dados.email, dados.senha, LocalDate.parse(dados.dataNascimento));
                UsuarioVO usuarioCadastrado = usuarioBO.cadastrar(novoUsuario, dados.cpf);
                ctx.status(201).json(usuarioCadastrado);
            } catch (Exception e) { ctx.status(400).result(e.getMessage()); }
        });

        // Feed de Postagens Geral
        app.get("/api/posts", ctx -> {
            try { 
                ctx.status(200).json(postDAO.listarTodos()); 
            } catch (Exception e) { 
                ctx.status(500).result(e.getMessage()); 
            }
        });

        app.post("/api/posts", ctx -> {
            try {
                DadosPost dados = ctx.bodyAsClass(DadosPost.class);
                PostVO postCriado = new PostVO(0, dados.texto, dados.autor);
                
                // Configuração Polimórfica com base no envio do Frontend
                if (dados.destinoTipo != null && "COMUNIDADE".equalsIgnoreCase(dados.destinoTipo)) {
                    ComunidadeVO c = new ComunidadeVO();
                    c.setId(dados.destinoId);
                    postCriado.setDestino(c);
                    postCriado.setDestinoTipo("COMUNIDADE");
                } else {
                    postCriado.setDestino(dados.autor);
                    postCriado.setDestinoTipo("USUARIO");
                }
                
                // Validação de segurança via BO
                usuarioBO.criarPostagem(dados.autor, dados.texto); 
                PostVO salvo = postDAO.salvar(postCriado);
                
                ctx.status(201).json(salvo);
            } catch (Exception e) { 
                ctx.status(400).result(e.getMessage()); 
            }
        });

        // Interações Sociais: Curtir e Responder
        app.post("/api/posts/{id}/curtir", ctx -> {
            try {
                int postId = Integer.parseInt(ctx.pathParam("id"));
                DadosInteracao dados = ctx.bodyAsClass(DadosInteracao.class);
                boolean curtiu = usuarioBO.processarCurtida(dados.usuarioId, postId);
                ctx.status(200).result(curtiu ? "curtiu" : "descurtiu");
            } catch (Exception e) { ctx.status(400).result(e.getMessage()); }
        });

        app.post("/api/posts/{id}/responder", ctx -> {
            try {
                int postId = Integer.parseInt(ctx.pathParam("id"));
                DadosInteracao dados = ctx.bodyAsClass(DadosInteracao.class);
                usuarioBO.adicionarComentario(dados.usuarioId, postId, dados.texto);
                ctx.status(201).result("Comentário gravado com sucesso");
            } catch (Exception e) { ctx.status(400).result(e.getMessage()); }
        });

        // NOVAS ROTAS DA CAMADA DE COMUNIDADES
        app.get("/api/comunidades", ctx -> {
            try {
                ctx.status(200).json(comunidadeDAO.listarTodas());
            } catch (Exception e) {
                ctx.status(500).result(e.getMessage());
            }
        });

        app.post("/api/comunidades", ctx -> {
            try {
                DadosNovaComunidade dados = ctx.bodyAsClass(DadosNovaComunidade.class);
                if (dados.nome == null || dados.nome.trim().isEmpty()) {
                    throw new Exception("O nome da comunidade é obrigatório.");
                }
                ComunidadeVO novaCom = new ComunidadeVO(0, dados.nome, dados.descricao);
                ComunidadeVO salva = comunidadeDAO.salvar(novaCom);
                ctx.status(201).json(salva);
            } catch (Exception e) { // Correção do caractere corrompido efetuada aqui
                ctx.status(400).result(e.getMessage());
            }
        });
    } 

    // Classes auxiliares/DTOs para mapeamento de JSON com construtores padrão
    public static class DadosLogin { 
        public String email; 
        public String senha; 
        public DadosLogin() {}
    }
    
    public static class DadosCadastro { 
        public String nome; 
        public String email; 
        public String senha; 
        public String dataNascimento; 
        public String cpf; 
        public DadosCadastro() {}
    }
    
    public static class DadosInteracao { 
        public int usuarioId; 
        public String texto; 
        public DadosInteracao() {}
    }
    
    public static class DadosPost { 
        public String texto; 
        public UsuarioVO autor; 
        public String destinoTipo; 
        public int destinoId; 
        public DadosPost() {}
    }

    public static class DadosNovaComunidade {
        public String nome;
        public String descricao;
        public DadosNovaComunidade() {}
    }
}