package com.vo;

import java.sql.Timestamp;

public abstract class Perfil {
    protected int id;
    protected String nome;
    protected Timestamp dataCriacao;

    public Perfil() {}

    public Perfil(int id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public Timestamp getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(Timestamp dataCriacao) { this.dataCriacao = dataCriacao; }
}