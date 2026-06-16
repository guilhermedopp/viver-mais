const CACHE_NAME = 'viver-mais-v1';

self.addEventListener('fetch', (event) => {
    // Apenas intercepta pedidos GET (não bloqueia formulários/redirecionamentos)
    if (event.request.method !== 'GET') return;

    event.respondWith(
        fetch(event.request)
            .catch(() => caches.match(event.request))
    );
});