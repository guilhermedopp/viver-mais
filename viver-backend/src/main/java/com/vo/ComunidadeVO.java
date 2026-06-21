package com.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ComunidadeVO extends Perfil {
    private String descricao;
    private String fotoGrupo;   // base64 da foto/ícone do grupo
    private String papel;       // papel do usuário logado no grupo (ADMIN/MEMBRO)

    public ComunidadeVO() { super(); }

    public ComunidadeVO(int id, String nome, String descricao) {
        super(id, nome);
        this.descricao = descricao;
    }

    @Override
    public String getTipo() { return "COMUNIDADE"; }

    public String getDescricao()           { return descricao; }
    public void   setDescricao(String d)   { this.descricao = d; }
    
    public String getFotoGrupo()           { return fotoGrupo; }
    public void   setFotoGrupo(String f)   { this.fotoGrupo = f; }
    
    public String getPapel()               { return papel; }
    public void   setPapel(String p)       { this.papel = p; }
}