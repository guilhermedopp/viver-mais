# VIVER+

Rede social de partilha de momentos e socialização desenvolvida com foco em **inclusão digital e acessibilidade para a terceira idade**. Interface PWA minimalista de alto contraste, com backend Java e API REST completa.

---

## Funcionalidades

| Módulo | Descrição |
|---|---|
| Feed | Publicar textos e fotos, curtir/reagir (❤️ Curtida / 🫂 Abraço / 👏 Parabéns), comentar, filtrar por "todos" ou "seguindo"; posts de comunidades exibem badge identificador e são filtrados corretamente do feed geral |
| Pesquisa | Barra de busca de usuários por nome ou @apelido no feed |
| Perfil próprio | Foto, @apelido editável, estatísticas (posts, seguidores, seguindo) |
| Perfil público | Ver perfil de outro usuário, seguir/deixar de seguir, abrir chat direto |
| Chat privado | Mensagens diretas entre usuários com lista de conversas e badge de não lidas |
| Comunidades | Criar grupos, convidar membros (convite pendente/aceito/recusado), chat exclusivo do grupo, editar grupo (admin) |
| Posts em grupos | Publicar no feed de uma comunidade específica (`destinoTipo=COMUNIDADE`) |
| Notificações | Avisos de reações, comentários, novos seguidores, convites de grupos — tempo real via WebSocket |
| Autenticação | Login com e-mail/senha (BCrypt) ou Google OAuth 2.0, com JWT de 24h |
| PWA | Instalável no celular/desktop, funciona offline via Service Worker (estratégia network-first) |

---

## Arquitetura e Conceitos de POO

### Hierarquia de Classes

```
Perfil (abstract)
├── UsuarioVO          — usuário com senha, nickname, foto, seguidores
└── ComunidadeVO       — grupo com membros e administrador

MensagemBase (abstract)
├── MensagemVO         — mensagem privada entre dois usuários
└── MensagemGrupoVO    — mensagem no chat de uma comunidade

Reacao (abstract)      — Factory Method: Reacao.criar(String tipo)
├── Curtida            — ❤️
├── Abraco             — 🫂
└── Parabens           — 👏
```

### Padrões de Projeto

- **Observer** — `UsuarioVO` é o _Subject_; `Seguidor` é o _Observer_ concreto. Ao publicar um post, cada seguidor é notificado via DB e WebSocket em tempo real.
- **Factory Method** — `Reacao.criar(String tipo)` instancia a subclasse correta sem expor os construtores.
- **DAO (Data Access Object)** — cada entidade tem seu DAO com `PreparedStatement`, isolando todo acesso ao banco.
- **BO (Business Object)** — regras de negócio centralizadas (`UsuarioBO`): validação de idade (60+), comprimento de post (máx. 500), bloqueio de links externos, hash BCrypt.

### Camadas

```
Controller (Main.java / Javalin)
    └── BO  (com.bo)
         └── DAO (com.dao)
              └── MySQL (viver_db)
```

---

## Stack Tecnológica

**Backend**
- Java 21, Maven
- [Javalin 6](https://javalin.io/) — framework HTTP leve
- [Auth0 JWT](https://github.com/auth0/java-jwt) — autenticação stateless HS256
- [jBCrypt](https://www.mindrot.org/projects/jBCrypt/) — hash de senhas
- [dotenv-java](https://github.com/cdimascio/dotenv-java) — variáveis de ambiente via `.env`
- MySQL 8 com `utf8mb4`

**Frontend**
- HTML5, CSS3, JavaScript puro (sem frameworks)
- PWA: Service Worker (`sw.js` v5), Web App Manifest
- Google Identity Services (OAuth 2.0 popup)
- WebSocket nativo para notificações em tempo real

---

## Estrutura de Ficheiros

```
viver-mais/
├── viver-backend/
│   ├── src/main/java/
│   │   ├── Main.java                   # Ponto de entrada, rotas da API, DTOs
│   │   └── com/
│   │       ├── bo/  UsuarioBO.java
│   │       ├── dao/ ConexaoDB, PostDAO, UsuarioDAO, ComunidadeDAO,
│   │       │        MensagemDAO, MensagemGrupoDAO, SeguidorDAO, NotificacaoDAO
│   │       ├── vo/  Perfil, UsuarioVO, ComunidadeVO, PostVO, RespostaVO,
│   │       │        MensagemBase, MensagemVO, MensagemGrupoVO,
│   │       │        Reacao (+ Curtida, Abraco, Parabens),
│   │       │        NotificacaoVO, EstatisticasPerfilVO
│   │       └── observer/ Observer (interface), Seguidor
│   ├── script.sql                      # Schema completo + dados de teste
│   ├── .env                            # Credenciais (gitignored)
│   └── pom.xml
└── viver-frontend/
    ├── index.html          # Login
    ├── cadastro.html       # Registo
    ├── completar-perfil.html  # Completar perfil Google
    ├── feed.html           # Feed principal + pesquisa
    ├── perfil.html         # Perfil próprio
    ├── perfil-publico.html # Perfil de outro utilizador
    ├── publicar.html       # Criar post (texto + foto)
    ├── notificacoes.html   # Centro de notificações
    ├── chat.html           # Chat privado e de grupo
    ├── comunidades.html    # Gerir comunidades
    ├── js/
    │   ├── api.js          # Interceptor fetch (JWT), toast, acessibilidade
    │   └── pwa-setup.js    # Registo do Service Worker
    ├── css/ style.css
    ├── sw.js               # Service Worker (network-first, cache v5)
    ├── manifest.json
    └── package.json        # Scripts npm (dev, watch, test)
testes.mjs                  # Suite de testes automatizados (Playwright)
```

---

## API REST

**Base URL:** `http://localhost:8080/api`

Todas as rotas exceto as públicas abaixo exigem o header `Authorization: Bearer <token>`.

**Rotas públicas:**
- `POST /login` · `POST /cadastro` · `POST /auth/google` · `POST /auth/completar-perfil`
- `GET /usuarios/nickname/{nick}/disponivel`

### Auth
| Método | Rota | Descrição |
|---|---|---|
| POST | `/login` | Retorna `{ token, usuario }` |
| POST | `/cadastro` | Cria conta (valida senha, idade ≥ 60, e-mail único) |
| POST | `/auth/google` | Login/cadastro via Google credential |
| POST | `/auth/completar-perfil` | Define nickname e data de nascimento pós-Google |

### Feed / Posts
| Método | Rota | Descrição |
|---|---|---|
| GET | `/posts?usuarioId=&filtro=` | Lista posts (`filtro=seguindo` ou todos) |
| POST | `/posts` | Cria post (texto, imagem base64, destinoTipo, destinoId) |
| POST | `/posts/{id}/ver` | Marca post como visto |
| POST | `/posts/{id}/reagir` | Alterna reação (`tipo`: curtida / abraco / parabens) |
| POST | `/posts/{id}/responder` | Adiciona comentário |

### Usuários
| Método | Rota | Descrição |
|---|---|---|
| GET | `/usuarios` | Lista todos |
| GET | `/usuarios/buscar?email=` / `?nickname=` | Busca por e-mail ou @apelido |
| GET | `/usuarios/{id}` | Retorna `{ usuario, posts, estatisticas }` |
| GET | `/usuarios/{id}/estatisticas` | Totais de posts, seguidores, seguindo |
| GET | `/usuarios/{id}/sigo?usuarioId=` | Verifica se está seguindo |
| GET | `/usuarios/{id}/amigos` | Lista amigos em comum (segue e é seguido) |
| POST | `/usuarios/{id}/seguir` | Seguir ou deixar de seguir |
| POST | `/usuarios/{id}/foto` | Atualiza foto de perfil (base64) |
| PUT | `/usuarios/{id}/nickname` | Atualiza @apelido |

### Notificações
| Método | Rota | Descrição |
|---|---|---|
| GET | `/notificacoes/{uid}` | Lista todas |
| GET | `/notificacoes/{uid}/nao-lidas` | Retorna `{ total }` |
| POST | `/notificacoes/{uid}/ler` | Marca todas como lidas |

### Chat Privado
| Método | Rota | Descrição |
|---|---|---|
| GET | `/chat/contatos/{uid}` | Lista conversas abertas |
| GET | `/chat/nao-lidas/{uid}` | Retorna `{ total }` de msgs não lidas |
| GET | `/chat/{uidA}/{uidB}` | Retorna histórico e marca como lidas |
| POST | `/chat` | Envia mensagem (`remetenteId`, `destinatarioId`, `conteudo`) |

### Comunidades
| Método | Rota | Descrição |
|---|---|---|
| GET | `/comunidades?usuarioId=` | Lista grupos do utilizador |
| POST | `/comunidades` | Cria grupo |
| PUT | `/comunidades/{id}` | Edita grupo (apenas admin) |
| GET | `/comunidades/{id}/membros` | Lista membros |
| POST | `/comunidades/{id}/convidar` | Envia convite (notifica convidado) |
| POST | `/convites/{id}/responder` | Aceitar ou recusar convite |
| GET | `/convites/pendentes/{uid}` | Lista convites pendentes |
| GET | `/comunidades/{id}/mensagens?usuarioId=` | Chat do grupo |
| POST | `/comunidades/{id}/mensagens` | Envia mensagem no grupo |

### WebSocket
| Rota | Descrição |
|---|---|
| `ws://localhost:8080/ws/notificacoes/{uid}` | Recebe `"NOVA_NOTIFICACAO"` em tempo real quando o Observer dispara |

---

## Como Executar

### Pré-requisitos
- Java 21+
- Maven 3.8+
- MySQL 8+
- Node.js 18+ (para `npx serve`)

### 1. Base de dados

```sql
-- No cliente MySQL (ex: MySQL Workbench ou terminal):
source viver-backend/script.sql
```

Isto cria o banco `viver_db` com todas as tabelas e dois utilizadores de teste.

### 2. Variáveis de ambiente

Crie o ficheiro `viver-backend/.env`:

```env
DB_URL=jdbc:mysql://localhost:3306/viver_db?allowPublicKeyRetrieval=true&useSSL=false&characterEncoding=UTF-8&useUnicode=true
DB_USER=root
DB_PASS=sua_senha_mysql
JWT_SECRET=uma_chave_secreta_longa
```

### 3. Iniciar

```bash
npm run dev
```

Inicia automaticamente o backend (porta **8080**) e o frontend (porta **3000**).

Acesse: **http://localhost:3000**

### Comandos disponíveis

| Comando | O que faz |
|---|---|
| `npm run dev` | Inicia backend + frontend em paralelo |
| `npm run watch` | Backend com recompilação automática ao salvar |
| `npm run test` | Executa a suite de testes automatizados (Playwright) |
| `mvn clean compile exec:java -Dexec.mainClass=Main` | Apenas o backend |
| `npx serve viver-frontend` | Apenas o frontend |

---

## Contas de Teste

Após executar o `script.sql`:

| Nome | E-mail | Senha |
|---|---|---|
| Dona Maria | maria@viver.com | Teste123 |
| Seu José | jose@viver.com | Teste123 |

---

## Testes Automatizados

O projeto inclui uma suite de testes end-to-end (`testes.mjs`) baseada em [Playwright](https://playwright.dev/), cobrindo 74 cenários de API e UI.

**Pré-requisito:** backend em `:8080` e frontend em `:3000` já iniciados.

```bash
# Em terminais separados:
npm run backend
npm run frontend

# Depois:
npm run test
```

| Seção | O que é testado |
|---|---|
| Autenticação | Login, cadastro, token JWT |
| Feed / Posts | Criar post, reagir, comentar, filtrar por "seguindo" |
| Perfil | Atualizar foto, @apelido, estatísticas |
| Seguir | Seguir/deixar de seguir, verificação de estado |
| Chat privado | Enviar mensagem, marcar como lida, listar conversas |
| Comunidades | Criar grupo, convidar membro, aceitar convite, chat de grupo |
| Notificações | Listar, marcar como lida, contador |
| UI (Playwright) | Fluxo de login, navegação, abertura de chat por URL |

---

## Segurança

- **JWT HS256** com expiração de 24h; filtro `before("/api/*")` rejeita requisições sem token.
- **BCrypt** (10 rounds) para hash de senhas; nunca armazenada em texto simples.
- **PreparedStatement** em todos os DAOs — imune a SQL Injection.
- **Google OAuth** com validação do campo `aud` contra o Client ID do projeto.
- **Validação de idade** no cadastro e no login Google: mínimo 60 anos.
- **Bloqueio de links externos** em posts: `http://`, `https://` e "clique aqui" são rejeitados.
- Limite de **500 caracteres** por post e **280** por comentário.
- Requisições aceitas até **20 MB** (suporte a fotos em base64).

---

*Desenvolvido como projeto académico para a disciplina de Programação Orientada a Objetos — IFAL 2026.1*
