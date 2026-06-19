// Aguarda que o formulário seja submetido
const formLogin = document.getElementById('form-login');

if (formLogin) {
    formLogin.addEventListener('submit', async function(event) {
        // Evita que a página recarregue ao clicar no botão
        event.preventDefault();

        // 1. Captura os dados que o idoso digitou nos campos
        const email = document.getElementById('email').value;
        const senha = document.getElementById('senha').value;

        try {
            // 2. Envia um pacote de dados (JSON) para o servidor Java
            const resposta = await fetch('http://localhost:8080/api/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ email: email, senha: senha })
            });
<<<<<<< Updated upstream

            // 3. Verifica a resposta do servidor
            if (resposta.ok) {
                // Sucesso: O Java confirmou o login
                const dadosUsuario = await resposta.json();
                
                // O código novo foi inserido AQUI de forma segura:
                // Guarda o utilizador no navegador para sabermos quem está logado no Feed
                localStorage.setItem('usuarioLogado', JSON.stringify(dadosUsuario));
                
                alert('Bem-vindo(a) ao VIVER+, ' + dadosUsuario.nome + '!');
                window.location.href = 'feed.html'; // Avança de forma dinâmica para o feed
                
=======
            
            if (resp.ok) {
                const usuario = await resp.json();
                msg.textContent = 'Conta criada com sucesso, ' + usuario.nome + '!';
                msg.className = 'mensagem mensagem-sucesso';
                localStorage.setItem('emailRecenteCadastro', email);
                setTimeout(() => window.location.href = 'login.html', 1500);
>>>>>>> Stashed changes
            } else {
                // Erro: E-mail ou palavra-passe incorretos (regra do UsuarioBO)
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
