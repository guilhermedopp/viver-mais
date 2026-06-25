// ═══════════════════════════════════════════════════════════════
// VIVER+ — api.js
// Responsabilidades:
//   1. Define API_URL e garante que TODOS os fetch() para o backend
//      incluem automaticamente o JWT (interceptor global)
//   2. Trata login por formulário e login por Google
//   3. Trata cadastro
//   4. Modo de acessibilidade (fonte grande)
// ═══════════════════════════════════════════════════════════════

const API_URL = 'http://localhost:8080/api';

// ── BUG CORRIGIDO: Interceptor Global de JWT ─────────────────────
// Substitui window.fetch para injetar automaticamente o header
// Authorization em TODA chamada para o servidor VIVER+.
// Isso resolve o bug onde as páginas internas faziam fetch()
// diretamente sem incluir o JWT, recebendo 401 em todas as rotas.
(function () {
    const fetchOriginal = window.fetch.bind(window);
    window.fetch = function (url, opcoes = {}) {
        const token = localStorage.getItem('jwtToken');
        if (token && typeof url === 'string' && url.includes('localhost:8080')) {
            opcoes.headers = {
                ...opcoes.headers,
                'Authorization': `Bearer ${token}`
            };
        }
        return fetchOriginal(url, opcoes);
    };
})();

// ── ACESSIBILIDADE: Modo Fonte Grande ────────────────────────────
// Restaura o modo ao carregar a página (persiste entre sessões)
if (localStorage.getItem('fonteGrande') === 'true') {
    document.body.classList.add('fonte-grande');
}

function alternarAcessibilidade() {
    const activo = document.body.classList.toggle('fonte-grande');
    localStorage.setItem('fonteGrande', activo);
    const btn = document.querySelector('[onclick="alternarAcessibilidade()"]');
    if (btn) btn.textContent = activo ? '🔍 Letra Normal' : '🔍 Letra Maior';
}

// ── LOGIN por formulário ──────────────────────────────────────────
const formLogin = document.getElementById('form-login');
if (formLogin) {
    formLogin.addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('email').value.trim();
        const senha = document.getElementById('senha').value;
        const msgEl = document.getElementById('msg-login');

        try {
            const res = await fetch(`${API_URL}/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, senha })
            });

            if (res.ok) {
                const data = await res.json();
                // Agora o backend devolve o token separado do objeto do utilizador
                localStorage.setItem('jwtToken', data.token);
                localStorage.setItem('usuarioLogado', JSON.stringify(data.usuario));
                window.location.href = 'feed.html';
            } else {
                const erro = await res.text();
                if (msgEl) { msgEl.textContent = erro; msgEl.style.display = 'block'; }
                else alert('Erro: ' + erro);
            }
        } catch (err) {
            const msg = 'Não foi possível conectar ao servidor VIVER+.';
            if (msgEl) { msgEl.textContent = msg; msgEl.style.display = 'block'; }
            else alert(msg);
        }
    });
}

// ── CADASTRO por formulário ───────────────────────────────────────
const formCadastro = document.getElementById('form-cadastro');
if (formCadastro) {
    formCadastro.addEventListener('submit', async (e) => {
        e.preventDefault();
        const senha    = document.getElementById('senha').value;
        const confirma = document.getElementById('confirmaSenha')?.value;
        const msgEl    = document.getElementById('msg-cadastro');

        if (confirma !== undefined && senha !== confirma) {
            if (msgEl) { msgEl.textContent = 'As senhas não coincidem!'; msgEl.style.display = 'block'; }
            else alert('As senhas não coincidem!');
            return;
        }

        const payload = {
            nome:           document.getElementById('nome')?.value?.trim(),
            nickname:       document.getElementById('nickname')?.value?.replace('@', '').trim(),
            email:          document.getElementById('email').value.trim(),
            dataNascimento: document.getElementById('dataNascimento')?.value,
            senha
        };

        try {
            const res = await fetch(`${API_URL}/cadastro`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (res.ok) {
                // Preenche o e-mail automaticamente na página de login
                localStorage.setItem('emailRecenteCadastro', payload.email);
                alert('Conta criada com sucesso! Agora pode entrar.');
                window.location.href = 'index.html';
            } else {
                const erro = await res.text();
                if (msgEl) { msgEl.textContent = erro; msgEl.style.display = 'block'; }
                else alert('Erro: ' + erro);
            }
        } catch (err) {
            const msg = 'Não foi possível conectar ao servidor VIVER+.';
            if (msgEl) { msgEl.textContent = msg; msgEl.style.display = 'block'; }
            else alert(msg);
        }
    });
}
