const API_URL = 'http://localhost:8080/api';

// ── Validação de CPF (Algoritmo oficial — Módulo 11) ──────────────────
function validarCPF(cpf) {
    cpf = cpf.replace(/[^\d]+/g, '');
    if (cpf.length !== 11 || /^(\d)\1{10}$/.test(cpf)) return false;
    let add = 0;
    for (let i = 0; i < 9; i++) add += parseInt(cpf[i]) * (10 - i);
    let rev = 11 - (add % 11);
    if (rev >= 10) rev = 0;
    if (rev !== parseInt(cpf[9])) return false;
    add = 0;
    for (let i = 0; i < 10; i++) add += parseInt(cpf[i]) * (11 - i);
    rev = 11 - (add % 11);
    if (rev >= 10) rev = 0;
    return rev === parseInt(cpf[10]);
}

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

// ── CADASTRO ──────────────────────────────────────────────────────────
const formCadastro = document.getElementById('form-cadastro');
if (formCadastro) {
    formCadastro.addEventListener('submit', async (e) => {
        e.preventDefault();
        const nome           = document.getElementById('nome').value.trim();
        const cpf            = document.getElementById('cpf').value.replace(/\D/g, '');
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
        if (!validarCPF(cpf)) {
            msg.textContent = 'CPF inválido. Verifique os números digitados.';
            msg.className = 'mensagem mensagem-erro'; return;
        }

        msg.textContent = 'A validar dados...';
        msg.className = 'mensagem';

        try {
            const resp = await fetch(`${API_URL}/cadastro`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ nome, email, senha, dataNascimento, cpf })   // ← CPF enviado
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