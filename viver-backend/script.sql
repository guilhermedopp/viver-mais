-- Cria a base de dados do projeto VIVER+
CREATE DATABASE viver_db;

-- Seleciona a base de dados para uso
USE viver_db;

-- Cria a tabela de utilizadores
CREATE TABLE usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    senha VARCHAR(50) NOT NULL
);

-- Insere uma utilizadora de teste para podermos fazer o primeiro login
INSERT INTO usuarios (nome, email, senha) 
VALUES ('Dona Maria', 'maria@viver.com', '1234');