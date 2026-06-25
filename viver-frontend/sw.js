const CACHE_NAME = 'viver-mais-v4';

const ARQUIVOS_CACHE = [
  './',
  './index.html',
  './cadastro.html',
  './feed.html',
  './perfil.html',
  './perfil-publico.html',
  './notificacoes.html',
  './comunidades.html',
  './chat.html',
  './publicar.html',
  './completar-perfil.html',
  './css/style.css',
  './js/api.js',
  './js/pwa-setup.js',
  './manifest.json',
  './img/logo.png'
];

// Instala e armazena os arquivos estáticos em cache
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      console.log('[SW] Cache aberto, guardando arquivos estáticos...');
      return cache.addAll(ARQUIVOS_CACHE);
    }).then(() => self.skipWaiting())
  );
});

// Remove caches antigos quando uma nova versão do SW é ativada
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((nomes) =>
      Promise.all(
        nomes
          .filter((nome) => nome !== CACHE_NAME)
          .map((nome) => {
            console.log('[SW] Removendo cache antigo:', nome);
            return caches.delete(nome);
          })
      )
    ).then(() => self.clients.claim())
  );
});

// Estratégia: cache-first para estáticos, network-first para API
self.addEventListener('fetch', (event) => {
  if (event.request.method !== 'GET') return;

  const url = new URL(event.request.url);

  // Chamadas à API: sempre vai à rede (dados em tempo real)
  if (url.pathname.startsWith('/api/') || url.hostname.includes('googleapis')) {
    event.respondWith(fetch(event.request));
    return;
  }

  // Arquivos estáticos: rede primeiro, cache como fallback (garante conteúdo atualizado)
  event.respondWith(
    fetch(event.request).then((respostaRede) => {
      if (respostaRede && respostaRede.status === 200) {
        const copia = respostaRede.clone();
        caches.open(CACHE_NAME).then((cache) => cache.put(event.request, copia));
      }
      return respostaRede;
    }).catch(() => {
      // Offline: tenta o cache como fallback
      return caches.match(event.request).then((respostaCache) => {
        return respostaCache || caches.match('./index.html');
      });
    })
  );
});