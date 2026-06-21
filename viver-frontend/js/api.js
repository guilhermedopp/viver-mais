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

// ── CADASTRO (Agora com Nickname!) ──────────────────────────────────
const formCadastro = document.getElementById('form-cadastro');
if (formCadastro) {
    formCadastro.addEventListener('submit', async (e) => {
        e.preventDefault();
        const nome           = document.getElementById('nome').value.trim();
        const nickname       = document.getElementById('nickname').value.trim().replace('@', ''); // Limpa o @
        const email          = document.getElementById('email').value.trim();
        const dataNascimento = document.getElementById('dataNascimento').value;
        const senha          = document.getElementById('senha').value;
        const confirmar      = document.getElementById('confirmaSenha').value;
        const msg            = document.getElementById('mensagem-cadastro') || { textContent: '', className: '' };

        if (senha !== confirmar) {
            msg.textContent = 'As senhas não coincidem.';
            msg.className = 'mensagem mensagem-erro'; return;
        }

        try {
            const resp = await fetch(`${API_URL}/cadastro`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                // Adicionamos o nickname aqui para bater com o backend
                body: JSON.stringify({ nome, nickname, email, senha, dataNascimento })
            });
            
            if (resp.ok) {
                alert('Conta criada com sucesso!');
                localStorage.setItem('emailRecenteCadastro', email);
                window.location.href = 'index.html';
            } else {
                alert('Erro: ' + await resp.text());
            }
        } catch (err) {
            alert('Não foi possível ligar ao servidor.');
        }
    });
}

// ── FUNÇÕES DE PERFIL (Para usar nas telas de edição) ────────────────
const api = {
    async atualizarNickname(usuarioId, novoNickname) {
        const res = await fetch(`${API_URL}/usuarios/${usuarioId}/nickname`, {
            method: 'PUT',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({ nickname: novoNickname.replace('@', '') })
        });
        return res.ok;
    },

    async atualizarFoto(usuarioId, base64) {
        const res = await fetch(`${API_URL}/usuarios/${usuarioId}/foto`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({ base64: base64 })
        });
        return res.ok;
    }
};