package com.vo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.observer.Observer;

public class UsuarioVO extends Perfil {
    private String email;
    private String senha;
    private LocalDate dataNascimento; 
    private List<Observer> seguidores = new ArrayList<>();

    // Construtor vazio exigido pelo Jackson
    public UsuarioVO() {
        super();
    }

    // Construtor completo utilizando reaproveitamento da Superclasse
    public UsuarioVO(int id, String nome, String email, String senha, LocalDate dataNascimento) {
        super(id, nome); // Inicializa o ID e Nome na classe Perfil
        this.email = email;
        this.senha = senha;
        this.dataNascimento = dataNascimento;
    }

    // Métodos do Observer
    public void adicionarSeguidor(Observer seguidor) {
        this.seguidores.add(seguidor);
    }

    public void notificarSeguidores(String mensagem) {
        for (Observer seguidor : seguidores) {
            seguidor.atualizar(mensagem); 
        }
    }

    // Getters e Setters específicos de Usuário
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    
    @JsonIgnore
    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }
}