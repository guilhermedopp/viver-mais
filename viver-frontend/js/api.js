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

