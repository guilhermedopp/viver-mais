# VIVER+ 🌟

O **VIVER+** é uma rede social de partilha de momentos e socialização, desenvolvida com foco total na **inclusão digital e acessibilidade para a terceira idade**. 

O projeto elimina barreiras tecnológicas através de uma interface PWA (Progressive Web App) minimalista, de alto contraste e navegação simplificada, sustentada por um backend robusto em Java.

---

## 🎯 Funcionalidades Principais

* **Socialização Digital:** Feed de momentos com suporte a texto e **partilha de fotografias**.
* **Comunicação Integrada:** Sistema de **chat em tempo real** para conversas privadas e chats exclusivos para cada **grupo (comunidade)**.
* **Gestão de Grupos:** Criação de comunidades com sistema de convites inteligente (pendente/aceite) e hierarquia de membros (Admin/Membro).
* **Notificações Inteligentes:** Sistema baseado no padrão *Observer* que alerta o utilizador sobre novas curtidas, comentários e convites.
* **Acessibilidade e Segurança:** Validação rigorosa de idade (exclusivo 60+), interface PWA instalável e bloqueio de links externos para proteção contra *phishing*.

---

## 🏗️ Arquitetura e Padrões (POO)

O projeto foi desenhado com uma separação rigorosa de responsabilidades, dividindo a aplicação em dois grandes ecossistemas (Frontend e Backend), utilizando os seguintes conceitos de POO no servidor:

* **Padrões de Desenho (Design Patterns):** Aplicação do padrão **Observer** para a gestão de seguidores e notificações.
* **Arquitetura em Camadas (Layered Pattern):**
  * **VO (Value Object):** Entidades de domínio encapsuladas (`UsuarioVO`, `PostVO`, `MensagemVO`, `ComunidadeVO`), com blindagem de dados sensíveis na serialização (`@JsonIgnore`).
  * **BO (Business Object):** Camada de regras de negócio, tratamento de datas (`LocalDate`) para cálculo de idade, e validações de segurança.
  * **DAO (Data Access Object):** Isolamento total da comunicação com a base de dados via JDBC, com proteção nativa contra *SQL Injection* (`PreparedStatement`).
  * **Controller:** Exposição da API REST utilizando o Javalin.

---

## 🛠️ Stack Tecnológica

* **Backend:** Java 17, Maven, Javalin (API REST), MySQL.
* **Frontend:** HTML5, CSS3, JavaScript (PWA), Service Workers (`sw.js`).
* **Automação:** Node.js (`package.json`) para orquestração de ambiente e *Hot Reload*.

---

## 🚀 Como Executar Localmente

1.  **Base de Dados:** Execute o `script.sql` na sua instância MySQL local.
2.  **Configurar Variável:** Defina a variável de ambiente `DB_PASSWORD` com a senha do seu MySQL.
3.  **Iniciar:** No terminal, execute:
    > `npm run dev`

O sistema iniciará automaticamente o servidor Java e o servidor web frontend.
*(Dica: Para desenvolver no Backend com agilidade, utilize o comando `npm run watch`).*

---
*Desenvolvido com foco na empatia e na tecnologia acessível.*