package com.observer;

import com.dao.NotificacaoDAO;
import com.vo.UsuarioVO;
import io.javalin.websocket.WsContext;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Cada seguidor É um Observer. Quando é notificado, salva no DB e envia WebSocket.
public class Seguidor implements Observer {

    // Mapa estático para gerir as conexões do WebSocket em tempo real
    public static Map<Integer, WsContext> sessoesWS = new ConcurrentHashMap<>();

    private UsuarioVO usuario;
    private NotificacaoDAO notificacaoDAO = new NotificacaoDAO();

    public Seguidor(UsuarioVO usuario) {
        this.usuario = usuario;
    }

    @Override
    public void atualizar(String mensagem) {
        System.out.println("[VIVER+ Observer] Notificação → " + usuario.getNome() + ": " + mensagem);

        try {
            // 1. Persiste a notificação no banco de dados (Aba Avisos)
            notificacaoDAO.salvar(usuario.getId(), mensagem);
            
            // 2. Padrão Observer via WebSocket: Dispara aviso em tempo real para o Frontend
            WsContext ctx = sessoesWS.get(usuario.getId());
            if (ctx != null && ctx.session.isOpen()) {
                ctx.send("NOVA_NOTIFICACAO");
            }
        } catch (Exception e) {
            System.err.println("[VIVER+ Observer] Erro ao salvar/enviar notificação: " + e.getMessage());
        }
    }

    public UsuarioVO getUsuario() {
        return usuario;
    }
}