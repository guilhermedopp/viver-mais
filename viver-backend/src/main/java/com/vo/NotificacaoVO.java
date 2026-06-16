package com.vo;

import java.util.Date;

public class NotificacaoVO {
    private int id;
    private String mensagem;
    private boolean lida;
    private Date dataCriacao;
    private int usuarioId;

    public NotificacaoVO() {}

    public NotificacaoVO(int id, String mensagem, boolean lida, Date dataCriacao, int usuarioId) {
        this.id = id;
        this.mensagem = mensagem;
        this.lida = lida;
        this.dataCriacao = dataCriacao;
        this.usuarioId = usuarioId;
    }

    public int getId()             { return id; }
    public String getMensagem()    { return mensagem; }
    public boolean isLida()        { return lida; }
    public Date getDataCriacao()   { return dataCriacao; }
    public int getUsuarioId()      { return usuarioId; }
    public void setLida(boolean l) { this.lida = l; }
}