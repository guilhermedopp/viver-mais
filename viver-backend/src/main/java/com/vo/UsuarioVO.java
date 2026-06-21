package com.vo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.observer.Observer;

// Esta anotação resolve o erro na criação de posts, ignorando o campo "tipo" vindo do frontend
@JsonIgnoreProperties(ignoreUnknown = true)
public class UsuarioVO extends Perfil {

    private String nickname;        // @apelido único
    private String email;
    private String senha;
    private String googleId;        // login via Google
    private String fotoPerfil;
    private LocalDate dataNascimento;
    private List<Observer> seguidores = new ArrayList<>();

    public UsuarioVO() {}

    public UsuarioVO(int id, String nome, String nickname, String email, String senha, LocalDate dataNascimento) {
        super(id, nome);
        this.nickname = nickname;
        this.email = email;
        this.senha = senha;
        this.dataNascimento = dataNascimento;
    }

    // Construtor sem nickname para retrocompatibilidade em partes antigas do código
    public UsuarioVO(int id, String nome, String email, String senha, LocalDate dataNascimento) {
        this(id, nome, null, email, senha, dataNascimento);
    }

    @Override
    public String getTipo() { return "USUARIO"; }

    public void adicionarSeguidor(Observer s)       { seguidores.add(s); }
    public void notificarSeguidores(String msg)      { seguidores.forEach(s -> s.atualizar(msg)); }

    public String getNickname()                      { return nickname; }
    public void   setNickname(String n)              { this.nickname = n; }
    public String getEmail()                         { return email; }
    public void   setEmail(String e)                 { this.email = e; }

    @JsonIgnore public String getSenha()             { return senha; }
    public void   setSenha(String s)                 { this.senha = s; }

    @JsonIgnore public String getGoogleId()          { return googleId; }
    public void   setGoogleId(String g)              { this.googleId = g; }

    public String getFotoPerfil()                    { return fotoPerfil; }
    public void   setFotoPerfil(String fp)           { this.fotoPerfil = fp; }

    @JsonIgnore public LocalDate getDataNascimento() { return dataNascimento; }
    public void   setDataNascimento(LocalDate d)     { this.dataNascimento = d; }
}