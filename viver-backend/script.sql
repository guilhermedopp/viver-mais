-- VIVER+ — Script Completo do Banco de Dados
DROP DATABASE IF EXISTS viver_db;
CREATE DATABASE viver_db;
USE viver_db;

-- ─────────────────────────────────────────
-- 1. TABELA DE USUÁRIOS
-- ─────────────────────────────────────────
CREATE TABLE usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    senha VARCHAR(50) NOT NULL,
    data_nascimento DATE NOT NULL,
    foto_perfil MEDIUMTEXT DEFAULT NULL
);

INSERT INTO usuarios (nome, email, senha, data_nascimento)
VALUES ('Dona Maria', 'maria@viver.com', '1234', '1950-05-20');

-- ─────────────────────────────────────────
-- 2. TABELA DE POSTAGENS (Destino Polimórfico)
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

INSERT INTO postagens (conteudo, usuario_id, destino_tipo, destino_id)
VALUES ('Olá! Que alegria estar aqui no VIVER+! 🌱', 1, 'USUARIO', 1);

-- ─────────────────────────────────────────
-- 3. TABELA DE COMUNIDADES
-- ─────────────────────────────────────────
CREATE TABLE comunidades (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    descricao TEXT,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ─────────────────────────────────────────
-- 4. RELACIONAMENTOS E INTERAÇÕES
-- ─────────────────────────────────────────
CREATE TABLE curtidas (
    usuario_id INT NOT NULL,
    post_id    INT NOT NULL,
    PRIMARY KEY (usuario_id, post_id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (post_id)    REFERENCES postagens(id) ON DELETE CASCADE
);

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
-- 5. SOCIAL (SEGUIDORES)
-- ─────────────────────────────────────────
CREATE TABLE seguidores (
    seguidor_id INT NOT NULL,
    seguido_id  INT NOT NULL,
    data_seguimento TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (seguidor_id, seguido_id),
    FOREIGN KEY (seguidor_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (seguido_id)  REFERENCES usuarios(id) ON DELETE CASCADE,
    CHECK (seguidor_id <> seguido_id)
);

-- ─────────────────────────────────────────
-- 6. SISTEMA DE NOTIFICAÇÕES (OBSERVER PATTERN)
-- ─────────────────────────────────────────
CREATE TABLE notificacoes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id   INT NOT NULL,
    mensagem     TEXT NOT NULL,
    lida         BOOLEAN DEFAULT FALSE,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

-- ─────────────────────────────────────────
-- 7. VISUALIZAÇÕES
-- ─────────────────────────────────────────
CREATE TABLE visualizacoes (
    usuario_id INT NOT NULL,
    post_id    INT NOT NULL,
    data_vista TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (usuario_id, post_id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (post_id)    REFERENCES postagens(id) ON DELETE CASCADE
);