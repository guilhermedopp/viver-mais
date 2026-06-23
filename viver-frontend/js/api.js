const API_URL = 'http://localhost:8080/api';

// ── SISTEMA DE SEGURANÇA (JWT Interceptor) ────────────────────────────
// Intercepta todos os pedidos para adicionar o Token de Segurança (JWT)
const originalFetch = window.fetch;
window.fetch = async function(resource, config = {}) {
    if (typeof resource === 'string' && resource.includes('/api/') && 
        !resource.includes('/login') && !resource.includes('/cadastro') && 
        !resource.includes('/auth/google') && !resource.includes('/disponivel')) {
        
        config.headers = config.headers || {};
        const token = localStorage.getItem('jwtToken');
        if (token) config.headers['Authorization'] = `Bearer ${token}`;
    }
    return originalFetch(resource, config);
};

// ── SISTEMA DE ACESSIBILIDADE (Fonte Grande) ──────────────────────────
function aplicarAcessibilidade() {
    if(localStorage.getItem('fonte-grande') === 'true') {
        document.body.classList.add('fonte-grande');
    } else {
        document.body.classList.remove('fonte-grande');
    }
}

window.alternarAcessibilidade = function() {
    const estadoAtual = localStorage.getItem('fonte-grande') === 'true';
    localStorage.setItem('fonte-grande', !estadoAtual);
    aplicarAcessibilidade();
};

// Aplica o tamanho de fonte correto assim que qualquer página abre
document.addEventListener('DOMContentLoaded', aplicarAcessibilidade);

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
                const data = await resp.json();
                // Agora o backend devolve o token separado do objeto do utilizador
                localStorage.setItem('jwtToken', data.token); 
                localStorage.setItem('usuarioLogado', JSON.stringify(data.usuario));
                window.location.href = 'feed.html';
            } else {
                alert('Aviso: ' + await resp.text());
            }
        } catch (err) {
            alert('Não foi possível conectar ao VIVER+. Verifique se o servidor Java está ligado.');
        }
    });
}

// ── CADASTRO (Com Nickname) ───────────────────────────────────────────
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
            if(msg.className !== undefined) {
                msg.textContent = 'As senhas não coincidem.';
                msg.className = 'mensagem mensagem-erro'; 
            } else {
                alert('As senhas não coincidem.');
            }
            return;
        }

        try {
            const resp = await fetch(`${API_URL}/cadastro`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
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

// ── FUNÇÕES DE PERFIL (Para usar nas telas de edição) ─────────────────
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