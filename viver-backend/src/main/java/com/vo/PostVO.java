package com.vo;

import java.util.Date;

public class PostVO {
    private int id;
    private String conteudo;
    private Date data;
    private UsuarioVO autor;
    private boolean visto; // Nova variável para filtro de Feed

    public PostVO(int id, String conteudo, UsuarioVO autor) {
        this.id = id;
        this.conteudo = conteudo;
        this.autor = autor;
        this.data = new Date();
        this.visto = false; // Por padrão, um post novo nunca foi visto
    }

    public void setData(Date data){
        this.data = data;
    }
    
    public void setVisto(boolean visto) {
        this.visto = visto;
    }

    // Getters
    public String getConteudo() { return conteudo; }
    public UsuarioVO getAutor() { return autor; }
    public Date getData() { return data; }
    public int getId() { return id; }
    public boolean isVisto() { return visto; }
}