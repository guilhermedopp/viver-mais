-- VIVER+ — Script Final do Banco de Dados
DROP DATABASE IF EXISTS viver_db;
CREATE DATABASE viver_db;
USE viver_db;

CREATE TABLE usuarios (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    nome             VARCHAR(100)  NOT NULL,
    nickname         VARCHAR(30)   UNIQUE,              -- @apelido único
    email            VARCHAR(100)  NOT NULL UNIQUE,
    senha            VARCHAR(255),                      -- NULL quando login é via Google
    google_id        VARCHAR(255)  UNIQUE,              -- ID retornado pelo Google OAuth
    data_nascimento  DATE,                              -- opcional para login Google (pedido depois)
    foto_perfil      MEDIUMTEXT    DEFAULT NULL
);

INSERT INTO usuarios (nome, nickname, email, senha, data_nascimento)
VALUES
  ('Dona Maria', 'donamaria', 'maria@viver.com', '1234', '1950-05-20'),
  ('Seu José',   'seujose',   'jose@viver.com',  '1234', '1945-03-10');

CREATE TABLE postagens (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    conteudo     TEXT           NOT NULL,
    imagem       MEDIUMTEXT     DEFAULT NULL,
    data_criacao TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    usuario_id   INT            NOT NULL,
    destino_tipo VARCHAR(20)    DEFAULT 'USUARIO',
    destino_id   INT,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

INSERT INTO postagens (conteudo, usuario_id)
VALUES ('Que bom estar aqui no VIVER+! 🌱', 1);

CREATE TABLE curtidas (
    usuario_id INT NOT NULL,
    post_id    INT NOT NULL,
    PRIMARY KEY (usuario_id, post_id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (post_id)    REFERENCES postagens(id) ON DELETE CASCADE
);

CREATE TABLE respostas (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    conteudo     TEXT      NOT NULL,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_id   INT       NOT NULL,
    post_id      INT       NOT NULL,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (post_id)    REFERENCES postagens(id) ON DELETE CASCADE
);

CREATE TABLE seguidores (
    seguidor_id     INT NOT NULL,
    seguido_id      INT NOT NULL,
    data_seguimento TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (seguidor_id, seguido_id),
    FOREIGN KEY (seguidor_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (seguido_id)  REFERENCES usuarios(id) ON DELETE CASCADE,
    CHECK (seguidor_id <> seguido_id)
);

CREATE TABLE notificacoes (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id   INT     NOT NULL,
    mensagem     TEXT    NOT NULL,
    lida         BOOLEAN DEFAULT FALSE,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

CREATE TABLE visualizacoes (
    usuario_id INT NOT NULL,
    post_id    INT NOT NULL,
    data_vista TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (usuario_id, post_id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (post_id)    REFERENCES postagens(id) ON DELETE CASCADE
);

CREATE TABLE comunidades (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    nome         VARCHAR(100) NOT NULL,
    descricao    TEXT,
    foto_grupo   MEDIUMTEXT   DEFAULT NULL,             -- foto/ícone do grupo
    criador_id   INT,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (criador_id) REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE TABLE membros_comunidade (
    usuario_id    INT  NOT NULL,
    comunidade_id INT  NOT NULL,
    papel         ENUM('ADMIN','MEMBRO') DEFAULT 'MEMBRO',
    data_entrada  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (usuario_id, comunidade_id),
    FOREIGN KEY (usuario_id)    REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (comunidade_id) REFERENCES comunidades(id) ON DELETE CASCADE
);

CREATE TABLE convites (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    comunidade_id INT NOT NULL,
    convidante_id INT NOT NULL,
    convidado_id  INT NOT NULL,
    status        ENUM('PENDENTE','ACEITO','RECUSADO') DEFAULT 'PENDENTE',
    data_criacao  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (comunidade_id) REFERENCES comunidades(id) ON DELETE CASCADE,
    FOREIGN KEY (convidante_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (convidado_id)  REFERENCES usuarios(id) ON DELETE CASCADE
);

CREATE TABLE mensagens (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    remetente_id     INT  NOT NULL,
    destinatario_id  INT  NOT NULL,
    conteudo         TEXT NOT NULL,
    lida             BOOLEAN   DEFAULT FALSE,
    data_criacao     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (remetente_id)   REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (destinatario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

CREATE TABLE mensagens_grupo (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    comunidade_id INT  NOT NULL,
    usuario_id    INT  NOT NULL,
    conteudo      TEXT NOT NULL,
    data_criacao  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (comunidade_id) REFERENCES comunidades(id) ON DELETE CASCADE,
    FOREIGN KEY (usuario_id)    REFERENCES usuarios(id) ON DELETE CASCADE
);