package com.vo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.observer.Observer;

public class UsuarioVO {
    private int id; 
    private String nome; 
    private String email;
    private String senha;
    private LocalDate dataNascimento; // Novo atributo de idade
    private List<Observer> seguidores = new ArrayList<>();

    // Construtor completo (para o Cadastro)
    public UsuarioVO(int id, String nome, String email, String senha, LocalDate dataNascimento) {
        this.id = id;
        this.nome = nome;
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

    // Getters
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenha() { return senha; }
    public int getId() { return id; }
    public LocalDate getDataNascimento() { return dataNascimento; }
}