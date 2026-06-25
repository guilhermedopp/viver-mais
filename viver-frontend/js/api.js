// ═══════════════════════════════════════════════════════════════
// VIVER+ — api.js
// Responsabilidades: URL da API, Segurança JWT e Acessibilidade.
// ═══════════════════════════════════════════════════════════════

const API_URL = 'http://localhost:8080/api';

(function () {
    const fetchOriginal = window.fetch.bind(window);
    window.fetch = function (url, opcoes = {}) {
        const token = localStorage.getItem('jwtToken');
        // Correção: Agora verifica '/api/' para enviar o token de segurança a partir de qualquer IP
        if (token && typeof url === 'string' && url.includes('/api/')) {
            opcoes.headers = {
                ...opcoes.headers,
                'Authorization': `Bearer ${token}`
            };
        }
        return fetchOriginal(url, opcoes);
    };
})();

// ── ACESSIBILIDADE: Modo Fonte Grande ────────────────────────────
if (localStorage.getItem('fonteGrande') === 'true') {
    document.body.classList.add('fonte-grande');
}

function alternarAcessibilidade() {
    const activo = document.body.classList.toggle('fonte-grande');
    localStorage.setItem('fonteGrande', activo);
    const btn = document.querySelector('[onclick="alternarAcessibilidade()"]');
    if (btn) btn.textContent = activo ? '🔍 Letra Normal' : '🔍 Letra Maior';
}

function mostrarToast(mensagem, tipo) {
    tipo = tipo || '';
    let ctn = document.getElementById('__toast_ctn__');
    if (!ctn) {
        ctn = document.createElement('div');
        ctn.id = '__toast_ctn__';
        ctn.className = 'toast-ctn';
        document.body.appendChild(ctn);
    }
    const t = document.createElement('div');
    t.className = 'toast' + (tipo ? ' ' + tipo : '');
    t.textContent = mensagem;
    ctn.appendChild(t);
    setTimeout(function () {
        t.classList.add('saindo');
        setTimeout(function () { t.remove(); }, 300);
    }, 3500);
}

