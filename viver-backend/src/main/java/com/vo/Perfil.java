package com.vo;

import java.sql.Timestamp;

// Classe abstrata: define o contrato comum para UsuarioVO e ComunidadeVO
public abstract class Perfil {
    protected int id;
    protected String nome;
    protected Timestamp dataCriacao;

    public Perfil() {}

    public Perfil(int id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    public abstract String getTipo();

    public int getId()                           { return id; }
    public void setId(int id)                    { this.id = id; }
    public String getNome()                      { return nome; }
    public void setNome(String nome)             { this.nome = nome; }
    public Timestamp getDataCriacao()            { return dataCriacao; }
    public void setDataCriacao(Timestamp ts)     { this.dataCriacao = ts; }
}