package com.vo;

public class ComunidadeVO extends Perfil {
    private String descricao;

    public ComunidadeVO() {
        super();
    }

    public ComunidadeVO(int id, String nome, String descricao) {
        super(id, nome);
        this.descricao = descricao;
    }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}