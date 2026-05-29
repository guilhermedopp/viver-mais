package com.view;

import javax.swing.*;
import java.awt.*;
import com.vo.UsuarioVO;
import com.bo.UsuarioBO;

public class CadastroView extends JFrame {
    private JTextField txtNome = new JTextField(15);
    private JTextField txtEmail = new JTextField(15);
    private JPasswordField txtSenha = new JPasswordField(15);
    private JButton btnSalvar = new JButton("CADASTRAR");

    public CadastroView() {
        setTitle("INSTA - Cadastro Acessível");
        setSize(400, 400);
        setLayout(new GridLayout(4, 1, 10, 10)); // Layout mais organizado

        // Interface para idosos: Fonte grande e legível
        Font fonteGrande = new Font("Arial", Font.BOLD, 22);
        txtNome.setFont(fonteGrande);
        txtEmail.setFont(fonteGrande);
        txtSenha.setFont(fonteGrande);
        btnSalvar.setFont(fonteGrande);

        JPanel panelNome = new JPanel(new FlowLayout());
        panelNome.add(new JLabel("Nome:"));
        panelNome.add(txtNome);

        JPanel panelEmail = new JPanel(new FlowLayout());
        panelEmail.add(new JLabel("E-mail:"));
        panelEmail.add(txtEmail);

        JPanel panelSenha = new JPanel(new FlowLayout());
        panelSenha.add(new JLabel("Senha:"));
        panelSenha.add(txtSenha);

        add(panelNome);
        add(panelEmail);
        add(panelSenha);
        add(btnSalvar);

        // Ação do botão de salvar
        btnSalvar.addActionListener(e -> {
            try {
                // Pega os dados reais dos campos de texto
                String senhaStr = new String(txtSenha.getPassword());
                UsuarioVO vo = new UsuarioVO(0, txtNome.getText(), txtEmail.getText(), senhaStr);
                
                // Valida as regras de negócio antes de salvar
                new UsuarioBO().cadastrar(vo); 
                
                JOptionPane.showMessageDialog(this, "Cadastro realizado com sucesso! Faça seu Login.");
                
                // Redireciona para a tela de Login e fecha o Cadastro
                new LoginView().setVisible(true);
                this.dispose(); 
                
            } catch (Exception ex) {
                // Exibe o erro de validação (ex: e-mail inválido, dados vazios) para o usuário
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
            }
        });
    }
}