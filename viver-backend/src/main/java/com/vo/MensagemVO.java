package com.vo;

import java.util.Date;

public class MensagemVO {
    private int id;
    private UsuarioVO remetente;
    private UsuarioVO destinatario;
    private String conteudo;
    private boolean lida;
    private Date dataCriacao;

    public MensagemVO() {}

    public MensagemVO(int id, UsuarioVO remetente, UsuarioVO destinatario, String conteudo, boolean lida, Date dataCriacao) {
        this.id = id;
        this.remetente = remetente;
        this.destinatario = destinatario;
        this.conteudo = conteudo;
        this.lida = lida;
        this.dataCriacao = dataCriacao;
    }

    public int getId()                   { return id; }
    public UsuarioVO getRemetente()       { return remetente; }
    public UsuarioVO getDestinatario()    { return destinatario; }
    public String getConteudo()           { return conteudo; }
    public boolean isLida()               { return lida; }
    public Date getDataCriacao()          { return dataCriacao; }
}