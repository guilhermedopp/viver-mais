package com.vo;

import java.util.Date;

public class MensagemGrupoVO extends MensagemBase {
    private int comunidadeId;

    public MensagemGrupoVO() {}

    public MensagemGrupoVO(int id, int comunidadeId, UsuarioVO autor,
                            String conteudo, Date dataCriacao) {
        super(id, conteudo, autor, dataCriacao);
        this.comunidadeId = comunidadeId;
    }

    @Override
    public String getTipo() { return "GRUPO"; }

    public int getComunidadeId()           { return comunidadeId; }
    public void setComunidadeId(int id)    { this.comunidadeId = id; }
}