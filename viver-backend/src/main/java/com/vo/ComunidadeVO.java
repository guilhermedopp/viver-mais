package com.vo;

public class ComunidadeVO extends Perfil {
    private String descricao;

    public ComunidadeVO() { super(); }

    public ComunidadeVO(int id, String nome, String descricao) {
        super(id, nome);
        this.descricao = descricao;
    }

    // Polimorfismo: ComunidadeVO diz que é do tipo "COMUNIDADE"
    @Override
    public String getTipo() { return "COMUNIDADE"; }

    public String getDescricao()               { return descricao; }
    public void   setDescricao(String d)       { this.descricao = d; }
}