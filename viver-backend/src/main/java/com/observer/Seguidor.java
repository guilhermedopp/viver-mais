package com.observer;

import com.dao.NotificacaoDAO;
import com.vo.UsuarioVO;

// Cada seguidor É um Observer. Quando é notificado, grava no banco de dados.
public class Seguidor implements Observer {

    private UsuarioVO usuario;
    private NotificacaoDAO notificacaoDAO = new NotificacaoDAO();

    public Seguidor(UsuarioVO usuario) {
        this.usuario = usuario;
    }

    @Override
    public void atualizar(String mensagem) {
        // Imprime no console do servidor (útil para ver logs)
        System.out.println("[VIVER+ Observer] Notificação → " + usuario.getNome() + ": " + mensagem);

        // Persiste a notificação no banco de dados para aparecer na aba "Avisos"
        try {
            notificacaoDAO.salvar(usuario.getId(), mensagem);
        } catch (Exception e) {
            System.err.println("[VIVER+ Observer] Erro ao salvar notificação: " + e.getMessage());
        }
    }

    public UsuarioVO getUsuario() {
        return usuario;
    }
}