package com.observer;

import com.vo.UsuarioVO;

public class Seguidor implements Observer {
    private UsuarioVO usuario;

    public Seguidor(UsuarioVO usuario) {
        this.usuario = usuario;
    }

    @Override
    public void atualizar(String mensagem) {
        // Simula o aviso de nova postagem ou mensagem [cite: 107, 109]
        System.out.println("Notificação para " + usuario.getNome() + ": " + mensagem);
    }
}