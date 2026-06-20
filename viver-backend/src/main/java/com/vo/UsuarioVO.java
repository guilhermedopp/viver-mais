package com.vo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.observer.Observer;

public class UsuarioVO extends Perfil {

    private String email;
    private String senha;
    private String fotoPerfil;
    private LocalDate dataNascimento;
    private List<Observer> seguidores = new ArrayList<>();

    public UsuarioVO() {}

    public UsuarioVO(int id, String nome, String email, String senha, LocalDate dataNascimento) {
        super(id, nome);
        this.email = email;
        this.senha = senha;
        this.dataNascimento = dataNascimento;
    }

    // Polimorfismo: UsuarioVO diz que é do tipo "USUARIO"
    @Override
    public String getTipo() { return "USUARIO"; }

    // ── Observer ────────────────────────────────────────────────────
    public void adicionarSeguidor(Observer seguidor) {
        this.seguidores.add(seguidor);
    }

    public void notificarSeguidores(String mensagem) {
        for (Observer s : seguidores) {
            s.atualizar(mensagem);
        }
    }

    // ── Getters / Setters ───────────────────────────────────────────
    public String getEmail()                         { return email; }
    public void   setEmail(String e)                 { this.email = e; }

    @JsonIgnore
    public String getSenha()                         { return senha; }
    public void   setSenha(String s)                 { this.senha = s; }

    public String getFotoPerfil()                    { return fotoPerfil; }
    public void   setFotoPerfil(String fp)           { this.fotoPerfil = fp; }

    @JsonIgnore
    public LocalDate getDataNascimento()             { return dataNascimento; }
    public void      setDataNascimento(LocalDate d)  { this.dataNascimento = d; }
}