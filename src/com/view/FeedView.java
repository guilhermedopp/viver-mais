package com.view;

import com.bo.UsuarioBO;
import com.vo.UsuarioVO;
import java.awt.*;
import javax.swing.*;

public class FeedView extends JFrame {
    public FeedView(UsuarioVO usuarioLogado) {
        setTitle("INSTA - Seu Feed");
        setSize(500, 600);
        setLayout(new BorderLayout());

        
        JLabel lblBemVindo = new JLabel("Olá, " + usuarioLogado.getNome(), SwingConstants.CENTER);
        lblBemVindo.setFont(new Font("Arial", Font.BOLD, 24));
        add(lblBemVindo, BorderLayout.NORTH);

        
        JTextArea areaPost = new JTextArea(5, 20);
        areaPost.setFont(new Font("Arial", Font.PLAIN, 18));
        JButton btnPostar = new JButton("COMPARTILHAR MOMENTO");
        btnPostar.setFont(new Font("Arial", Font.BOLD, 20));
        btnPostar.setBackground(Color.CYAN);

        btnPostar.addActionListener(e -> {
            try {
                new UsuarioBO().criarPostagem(usuarioLogado, areaPost.getText());
                JOptionPane.showMessageDialog(this, "Publicado com sucesso!");
                areaPost.setText("");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Aviso de Segurança", JOptionPane.WARNING_MESSAGE);
            }
        });

        JPanel painelPost = new JPanel(new BorderLayout());
        painelPost.add(new JLabel("O que você está pensando?"), BorderLayout.NORTH);
        painelPost.add(areaPost, BorderLayout.CENTER);
        painelPost.add(btnPostar, BorderLayout.SOUTH);

        add(painelPost, BorderLayout.CENTER);
    }
}