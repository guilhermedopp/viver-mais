const API_URL = 'http://localhost:8080/api';

// ===================== LOGIN =====================
const formLogin = document.getElementById('form-login');

if (formLogin) {
    formLogin.addEventListener('submit', async function(event) {
        // Evita que a página recarregue ao clicar no botão
        event.preventDefault();

        // 1. Captura os dados que o idoso digitou nos campos
        const email = document.getElementById('email').value.trim();
        const senha = document.getElementById('senha').value;

        try {
            // 2. Envia um pacote de dados (JSON) para o servidor Java
            const resposta = await fetch(`${API_URL}/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ email: email, senha: senha })
            });

            // 3. Verifica a resposta do servidor
            if (resposta.ok) {
                // Sucesso: o Java confirmou o login
                const dadosUsuario = await resposta.json();

                // Guarda o utilizador no navegador para sabermos quem está logado no Feed
                localStorage.setItem('usuarioLogado', JSON.stringify(dadosUsuario));

                alert('Bem-vindo(a) ao VIVER+, ' + dadosUsuario.nome + '!');
                window.location.href = 'feed.html'; // Avança de forma dinâmica para o feed

            } else {
                // Erro: e-mail ou senha incorretos (regra do UsuarioBO)
                const mensagemErro = await resposta.text();
                alert('Aviso: ' + mensagemErro);
            }

        } catch (erro) {
            // Se o servidor Java estiver desligado
            alert('Não foi possível conectar ao VIVER+. Verifique a sua internet ou contacte o suporte.');
            console.error('Erro na API:', erro);
        }
    });
}

// ===================== CADASTRO =====================
const formCadastro = document.getElementById('form-cadastro');

if (formCadastro) {
    formCadastro.addEventListener('submit', async function(event) {
        event.preventDefault();

        const nome = document.getElementById('nome').value.trim();
        const email = document.getElementById('email').value.trim();
        const senha = document.getElementById('senha').value;
        const mensagem = document.getElementById('mensagem-cadastro');

        if (mensagem) {
            mensagem.textContent = 'Cadastrando...';
            mensagem.className = 'mensagem';
        }

        try {
            const resposta = await fetch(`${API_URL}/cadastro`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ nome: nome, email: email, senha: senha })
            });

            if (resposta.ok) {
                const usuario = await resposta.json();

                if (mensagem) {
                    mensagem.textContent = 'Conta criada com sucesso, ' + usuario.nome + '!';
                    mensagem.className = 'mensagem mensagem-sucesso';
                }

                // Leva o usuário para a tela de login após o cadastro
                setTimeout(function() {
                    window.location.href = 'index.html';
                }, 1200);
            } else {
                const mensagemErro = await resposta.text();

                if (mensagem) {
                    mensagem.textContent = mensagemErro;
                    mensagem.className = 'mensagem mensagem-erro';
                } else {
                    alert('Aviso: ' + mensagemErro);
                }
            }
        } catch (erro) {
            if (mensagem) {
                mensagem.textContent = 'Não foi possível conectar ao VIVER+. Verifique se o servidor Java está ligado.';
                mensagem.className = 'mensagem mensagem-erro';
            } else {
                alert('Não foi possível conectar ao VIVER+. Verifique a sua internet ou contacte o suporte.');
            }

            console.error('Erro na API:', erro);
        }
    });
}
