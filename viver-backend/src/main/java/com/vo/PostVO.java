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
    private int totalCurtidas;
    private boolean curtidoPeloUsuario;
    private List<RespostaVO> respostas = new ArrayList<>();
    private Perfil destino;
    private String destinoTipo;

    public PostVO() {}

    public PostVO(int id, String conteudo, UsuarioVO autor) {
        this.id = id;
        this.conteudo = conteudo;
        this.autor = autor;
        this.data = new Date();
        this.visto = false;
        this.destino = autor;
        this.destinoTipo = "USUARIO";
    }

    // Getters
    public int getId()                    { return id; }
    public String getConteudo()           { return conteudo; }
    public Date getData()                 { return data; }
    public UsuarioVO getAutor()           { return autor; }
    public boolean isVisto()              { return visto; }
    public int getTotalCurtidas()         { return totalCurtidas; }
    public boolean isCurtidoPeloUsuario() { return curtidoPeloUsuario; }
    public List<RespostaVO> getRespostas(){ return respostas; }
    public Perfil getDestino()            { return destino; }
    public String getDestinoTipo()        { return destinoTipo; }

    // Setters
    public void setId(int id)                              { this.id = id; }
    public void setConteudo(String c)                      { this.conteudo = c; }
    public void setData(Date d)                            { this.data = d; }
    public void setAutor(UsuarioVO a)                      { this.autor = a; }
    public void setVisto(boolean v)                        { this.visto = v; }
    public void setTotalCurtidas(int t)                    { this.totalCurtidas = t; }
    public void setCurtidoPeloUsuario(boolean c)           { this.curtidoPeloUsuario = c; }
    public void setRespostas(List<RespostaVO> r)           { this.respostas = r; }
    public void setDestino(Perfil d)                       { this.destino = d; }
    public void setDestinoTipo(String t)                   { this.destinoTipo = t; }
}