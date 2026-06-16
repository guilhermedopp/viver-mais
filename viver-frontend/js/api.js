const API_URL = 'http://localhost:8080/api';

// ── LOGIN ─────────────────────────────────────────────────────────────
const formLogin = document.getElementById('form-login');
if (formLogin) {
    formLogin.addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('email').value.trim();
        const senha = document.getElementById('senha').value;
        try {
            const resp = await fetch(`${API_URL}/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, senha })
            });
            if (resp.ok) {
                localStorage.setItem('usuarioLogado', JSON.stringify(await resp.json()));
                window.location.href = 'feed.html';
            } else {
                alert('Aviso: ' + await resp.text());
            }
        } catch (err) {
            alert('Não foi possível conectar ao VIVER+. Verifique se o servidor Java está ligado.');
        }
    });
}

// ── CADASTRO (Simplificado - Sem CPF) ──────────────────────────────────
const formCadastro = document.getElementById('form-cadastro');
if (formCadastro) {
    formCadastro.addEventListener('submit', async (e) => {
        e.preventDefault();
        const nome           = document.getElementById('nome').value.trim();
        const email          = document.getElementById('email').value.trim();
        const dataNascimento = document.getElementById('dataNascimento').value;
        const senha          = document.getElementById('senha').value;
        const confirmar      = document.getElementById('confirmarSenha').value;
        const msg            = document.getElementById('mensagem-cadastro');

        // Validações no frontend
        if (senha !== confirmar) {
            msg.textContent = 'As senhas não coincidem. Verifique e tente novamente.';
            msg.className = 'mensagem mensagem-erro'; return;
        }

        msg.textContent = 'A validar dados...';
        msg.className = 'mensagem';

        try {
            const resp = await fetch(`${API_URL}/cadastro`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ nome, email, senha, dataNascimento }) // CPF removido
            });
            
            if (resp.ok) {
                const usuario = await resp.json();
                msg.textContent = 'Conta criada com sucesso, ' + usuario.nome + '!';
                msg.className = 'mensagem mensagem-sucesso';
                localStorage.setItem('emailRecenteCadastro', email);
                setTimeout(() => window.location.href = 'index.html', 1500);
            } else {
                msg.textContent = await resp.text();
                msg.className = 'mensagem mensagem-erro';
            }
        } catch (err) {
            msg.textContent = 'Não foi possível ligar ao servidor.';
            msg.className = 'mensagem mensagem-erro';
        }
    });
}