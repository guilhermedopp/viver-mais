package com.vo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.observer.Observer;

// Herda id e nome da classe abstrata Perfil (polimorfismo com ComunidadeVO)
public class UsuarioVO extends Perfil {

    private String email;
    private String senha;
    private String fotoPerfil;        // base64 da foto (pode ser null)
    private LocalDate dataNascimento;
    private List<Observer> seguidores = new ArrayList<>();

    // Construtor vazio exigido pelo Jackson
    public UsuarioVO() {}

    // Construtor completo
    public UsuarioVO(int id, String nome, String email, String senha, LocalDate dataNascimento) {
        super(id, nome);           // id e nome vão para Perfil
        this.email = email;
        this.senha = senha;
        this.dataNascimento = dataNascimento;
    }

    // ── Observer ──────────────────────────────────
    public void adicionarSeguidor(Observer seguidor) {
        this.seguidores.add(seguidor);
    }

    public void notificarSeguidores(String mensagem) {
        for (Observer s : seguidores) {
            s.atualizar(mensagem);
        }
    }

    // ── Getters e Setters ─────────────────────────
    public String getEmail()        { return email; }
    public void   setEmail(String e){ this.email = e; }

    @JsonIgnore
    public String getSenha()        { return senha; }
    public void   setSenha(String s){ this.senha = s; }

    public String getFotoPerfil()           { return fotoPerfil; }
    public void   setFotoPerfil(String fp)  { this.fotoPerfil = fp; }

    @JsonIgnore
    public LocalDate getDataNascimento()             { return dataNascimento; }
    public void      setDataNascimento(LocalDate d)  { this.dataNascimento = d; }
}