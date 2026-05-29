package com.view;

import javax.swing.*;
import java.awt.*;
import com.bo.UsuarioBO;
import com.vo.UsuarioVO;

public class LoginView extends JFrame {
    private JTextField txtEmail = new JTextField(15);
    private JPasswordField txtSenha = new JPasswordField(15);
    private JButton btnLogin = new JButton("ENTRAR");
    private JButton btnIrParaCadastro = new JButton("Ainda não tem conta? Cadastre-se");

    public LoginView() {
        setTitle("INSTA - Acesso Seguro");
        setSize(400, 350);
        setLayout(new GridLayout(4, 1, 10, 10));

        // Mantendo a interface acessível para idosos [cite: 128]
        Font fonteGrande = new Font("Arial", Font.BOLD, 20);
        txtEmail.setFont(fonteGrande);
        txtSenha.setFont(fonteGrande);
        btnLogin.setFont(fonteGrande);

        JPanel panelEmail = new JPanel(new FlowLayout());
        panelEmail.add(new JLabel("E-mail:"));
        panelEmail.add(txtEmail);

        JPanel panelSenha = new JPanel(new FlowLayout());
        panelSenha.add(new JLabel("Senha:"));
        panelSenha.add(txtSenha);

        add(panelEmail);
        add(panelSenha);
        
        JPanel panelBotoes = new JPanel(new GridLayout(2, 1, 5, 5));
        panelBotoes.add(btnLogin);
        panelBotoes.add(btnIrParaCadastro);
        add(panelBotoes);

        // Ação de Login
        btnLogin.addActionListener(e -> {
            try {
                String senhaStr = new String(txtSenha.getPassword());
                // Tenta fazer o login usando a regra de negócio [cite: 162]
                UsuarioVO usuarioLogado = new UsuarioBO().login(txtEmail.getText(), senhaStr);
                
                // Se passou, abre o Feed e fecha o Login
                new FeedView(usuarioLogado).setVisible(true);
                this.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro de Autenticação", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Ação para ir para a tela de Cadastro
        btnIrParaCadastro.addActionListener(e -> {
            new CadastroView().setVisible(true);
            this.dispose();
        });
    }
}