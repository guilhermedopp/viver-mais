package com.vo;

import java.util.Date;

public class MensagemGrupoVO {
    private int id;
    private int comunidadeId;
    private UsuarioVO autor;
    private String conteudo;
    private Date dataCriacao;

    public MensagemGrupoVO() {}

    public MensagemGrupoVO(int id, int comunidadeId, UsuarioVO autor, String conteudo, Date dataCriacao) {
        this.id = id;
        this.comunidadeId = comunidadeId;
        this.autor = autor;
        this.conteudo = conteudo;
        this.dataCriacao = dataCriacao;
    }

    public int getId()             { return id; }
    public int getComunidadeId()   { return comunidadeId; }
    public UsuarioVO getAutor()    { return autor; }
    public String getConteudo()    { return conteudo; }
    public Date getDataCriacao()   { return dataCriacao; }
}