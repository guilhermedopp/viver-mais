package com.vo;

import java.util.Date;

// Herança: campos e comportamentos comuns a todos os tipos de mensagem
public abstract class MensagemBase {
    protected int id;
    protected String conteudo;
    protected Date dataCriacao;
    protected UsuarioVO autor;

    public MensagemBase() {}

    public MensagemBase(int id, String conteudo, UsuarioVO autor, Date dataCriacao) {
        this.id = id;
        this.conteudo = conteudo;
        this.autor = autor;
        this.dataCriacao = dataCriacao;
    }

    // Cada subclasse informa se é mensagem privada ou de grupo
    public abstract String getTipo();

    public int getId()                   { return id; }
    public String getConteudo()          { return conteudo; }
    public Date getDataCriacao()         { return dataCriacao; }
    public UsuarioVO getAutor()          { return autor; }
    public void setDataCriacao(Date d)   { this.dataCriacao = d; }
    public void setAutor(UsuarioVO a)    { this.autor = a; }
    public void setConteudo(String c)    { this.conteudo = c; }
    public void setId(int id)            { this.id = id; }
}