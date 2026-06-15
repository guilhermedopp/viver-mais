package com.vo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PostVO {
    private int id;
    private String conteudo;
    private Date data;
    private UsuarioVO autor;
    private boolean visto; 
    private List<RespostaVO> respostas = new ArrayList<>();
    
    // ATRIBUTOS POLIMÓRFICOS
    private Perfil destino; 
    private String destinoTipo; // "USUARIO" ou "COMUNIDADE"

    public PostVO() {}

    public PostVO(int id, String conteudo, UsuarioVO autor) {
        this.id = id;
        this.conteudo = conteudo;
        this.autor = autor;
        this.data = new Date();
        this.visto = false;
        // Padrão: post vai para o feed do próprio autor
        this.destino = autor;
        this.destinoTipo = "USUARIO";
    }

    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getConteudo() { return conteudo; }
    public void setConteudo(String conteudo) { this.conteudo = conteudo; }

    public Date getData() { return data; }
    public void setData(Date data) { this.data = data; }

    public UsuarioVO getAutor() { return autor; }
    public void setAutor(UsuarioVO autor) { this.autor = autor; }

    public boolean isVisto() { return visto; }
    public void setVisto(boolean visto) { this.visto = visto; }

    public List<RespostaVO> getRespostas() { return respostas; }
    public void setRespostas(List<RespostaVO> respostas) { this.respostas = respostas; }

    public Perfil getDestino() { return destino; }
    public void setDestino(Perfil destino) { this.destino = destino; }

    public String getDestinoTipo() { return destinoTipo; }
    public void setDestinoTipo(String destinoTipo) { this.destinoTipo = destinoTipo; }
}