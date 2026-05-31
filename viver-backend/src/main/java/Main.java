import io.javalin.Javalin;
import com.bo.UsuarioBO;
import com.vo.UsuarioVO;

public class Main {
    public static void main(String[] args) {
        
        // 1. Inicia o servidor Javalin na porta 8080
        Javalin app = Javalin.create(config -> {
            // Permite que o nosso Frontend (HTML/JS) consiga comunicar com o Java sem ser bloqueado (CORS)
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> it.anyHost());
            });
        }).start(8080);

        System.out.println("Servidor Backend VIVER+ iniciado com sucesso na porta 8080!");

        // Instância do nosso Business Object (Regras de Negócio)
        UsuarioBO usuarioBO = new UsuarioBO();

        // 2. Rota que recebe os pedidos de Login do Frontend
        app.post("/api/login", ctx -> {
            try {
                // Captura o JSON enviado pelo JavaScript (email e senha)
                DadosLogin dados = ctx.bodyAsClass(DadosLogin.class);
                
                // Passa para a nossa regra de negócio validar no banco de dados
                UsuarioVO usuarioLogado = usuarioBO.login(dados.email, dados.senha);
                
                // Se deu certo, devolve os dados do utilizador com o status 200 (OK)
                ctx.status(200).json(usuarioLogado);
                
            } catch (Exception e) {
                // Se deu erro (ex: senha errada), devolve a mensagem com o status 401 (Não Autorizado)
                ctx.status(401).result(e.getMessage());
            }
        });
    }

    // Classe auxiliar simples para ajudar o Java a entender o JSON que vem do Frontend
    public static class DadosLogin {
        public String email;
        public String senha;
    }
}