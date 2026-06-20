package com.vo;

import java.util.Date;

public class MensagemVO extends MensagemBase {
    private UsuarioVO remetente;
    private UsuarioVO destinatario;
    private boolean lida;

    public MensagemVO() {}

    public MensagemVO(int id, UsuarioVO remetente, UsuarioVO destinatario,
                      String conteudo, boolean lida, Date dataCriacao) {
        super(id, conteudo, remetente, dataCriacao);
        this.remetente = remetente;
        this.destinatario = destinatario;
        this.lida = lida;
    }

    @Override
    public String getTipo() { return "PRIVADA"; }

    public UsuarioVO getRemetente()            { return remetente; }
    public UsuarioVO getDestinatario()         { return destinatario; }
    public boolean isLida()                    { return lida; }
    public void setRemetente(UsuarioVO r)      { this.remetente = r; }
    public void setDestinatario(UsuarioVO d)   { this.destinatario = d; }
    public void setLida(boolean l)             { this.lida = l; }
}