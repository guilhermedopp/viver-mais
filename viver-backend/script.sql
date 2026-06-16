-- VIVER+ — Script Completo do Banco de Dados
DROP DATABASE IF EXISTS viver_db;
CREATE DATABASE viver_db;
USE viver_db;

-- ─────────────────────────────────────────
-- TABELA DE USUÁRIOS
-- ─────────────────────────────────────────
CREATE TABLE usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    senha VARCHAR(50) NOT NULL,
    data_nascimento DATE NOT NULL,
    foto_perfil MEDIUMTEXT DEFAULT NULL   -- Foto em base64 (para o perfil)
);

INSERT INTO usuarios (nome, email, senha, data_nascimento)
VALUES ('Dona Maria', 'maria@viver.com', '1234', '1950-05-20');

-- ─────────────────────────────────────────
-- TABELA DE POSTAGENS (com destino polimórfico)
-- ─────────────────────────────────────────
CREATE TABLE postagens (
    id INT AUTO_INCREMENT PRIMARY KEY,
    conteudo TEXT NOT NULL,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_id INT NOT NULL,
    destino_tipo VARCHAR(20) DEFAULT 'USUARIO',
    destino_id INT,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

-- Post de exemplo para o feed não ficar vazio
INSERT INTO postagens (conteudo, usuario_id, destino_tipo, destino_id)
VALUES ('Olá! Que alegria estar aqui no VIVER+! 🌱', 1, 'USUARIO', 1);

-- ─────────────────────────────────────────
-- CURTIDAS
-- ─────────────────────────────────────────
CREATE TABLE curtidas (
    usuario_id INT NOT NULL,
    post_id    INT NOT NULL,
    PRIMARY KEY (usuario_id, post_id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (post_id)    REFERENCES postagens(id) ON DELETE CASCADE
);

-- ─────────────────────────────────────────
-- RESPOSTAS / COMENTÁRIOS
-- ─────────────────────────────────────────
CREATE TABLE respostas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    conteudo TEXT NOT NULL,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_id INT NOT NULL,
    post_id    INT NOT NULL,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (post_id)    REFERENCES postagens(id) ON DELETE CASCADE
);

-- ─────────────────────────────────────────
-- COMUNIDADES / GRUPOS
-- ─────────────────────────────────────────
CREATE TABLE comunidades (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    descricao TEXT,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ─────────────────────────────────────────
-- SEGUIDORES (para o Observer)
-- ─────────────────────────────────────────
CREATE TABLE seguidores (
    seguidor_id INT NOT NULL,
    seguido_id  INT NOT NULL,
    PRIMARY KEY (seguidor_id, seguido_id),
    FOREIGN KEY (seguidor_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (seguido_id)  REFERENCES usuarios(id) ON DELETE CASCADE,
    CHECK (seguidor_id <> seguido_id)
);

-- ─────────────────────────────────────────
-- NOTIFICAÇÕES (geradas pelo Observer)
-- ─────────────────────────────────────────
CREATE TABLE notificacoes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id   INT NOT NULL,               -- quem recebe a notificação
    mensagem     TEXT NOT NULL,
    lida         BOOLEAN DEFAULT FALSE,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

-- ─────────────────────────────────────────
-- VISUALIZAÇÕES (filtro vistos / não vistos)
-- ─────────────────────────────────────────
CREATE TABLE visualizacoes (
    usuario_id INT NOT NULL,
    post_id    INT NOT NULL,
    data_vista TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (usuario_id, post_id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (post_id)    REFERENCES postagens(id) ON DELETE CASCADE
);