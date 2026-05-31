package com.vo;

public class UsuarioVO {
    private int id; 
    private String nome; 
    private String email;
    private String senha; 

    public UsuarioVO(int id, String nome, String email, String senha) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senha = senha; 
    }

    // Getters e Setters (Encapsulamento) [cite: 141, 142]
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenha() { return senha; }
    public int getId() { return id; }
}
