package com.vo;

import java.util.Date;

public class PostVO {
    private int id;
    private String conteudo;
    private Date data;
    private UsuarioVO autor;

    public PostVO(int id, String conteudo, UsuarioVO autor) {
        this.id = id;
        this.conteudo = conteudo;
        this.autor = autor;
        this.data = new Date();
    }

    // Getters
    public String getConteudo() { return conteudo; }
    public UsuarioVO getAutor() { return autor; }
    public Date getData() { return data; }
}