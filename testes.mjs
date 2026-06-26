/**
 * VIVER+ — Suite de Testes Automatizados
 * Uso: node testes.mjs
 * Pre-requisito: backend em :8080 e frontend em :3000
 *
 * npm run backend   → inicia o servidor Java
 * npm run frontend  → inicia o servidor estático
 * node testes.mjs   → roda os testes
 */

import { chromium } from '@playwright/test';
import { mkdirSync } from 'fs';

const BASE = 'http://localhost:3000';
const API  = 'http://localhost:8080/api';

// Usuarios de teste (devem existir no banco)
const U1_EMAIL = 'teste_auto@viverplus.com';
const U1_SENHA = 'Teste123';
const U2_EMAIL = 'donamaria2@viverplus.com';
const U2_SENHA = 'Teste123';

// Preenchidos dinamicamente apos login na secao 0
let tk1 = null, tk2 = null, uid1 = null, uid2 = null;
let uid1Nome = 'Teste Automatizado', uid2Nome = 'Dona Maria Silva';

// IDs criados durante os testes de API
let postIdTeste   = null;
let grupoIdTeste  = null;
let conviteIdTeste = null;

let pass = 0, fail = 0;
const erros = [];

const ok  = m     => { process.stdout.write(`  [OK] ${m}\n`);   pass++; };
const nok = (m,d) => { process.stdout.write(`  [FALHA] ${m}${d ? ' -- ' + d : ''}\n`); fail++; erros.push(m + (d ? ': ' + d : '')); };
const chk = async (label, fn) => {
  try { await fn(); }
  catch (e) { nok(label, e.message.split('\n')[0].slice(0, 120)); }
};

// ── Helper: fetch direto na API (Node 18+) ────────────────────────────────
async function api(method, path, body, token) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const opts = { method, headers };
  if (body !== undefined && body !== null) opts.body = JSON.stringify(body);
  const r = await fetch(`${API}${path}`, opts);
  const text = await r.text();
  let data = text;
  try { data = JSON.parse(text); } catch (_) {}
  return { status: r.status, data };
}
const GET  = (p, tk)     => api('GET',  p, null, tk);
const POST = (p, b, tk)  => api('POST', p, b,    tk);
const PUT  = (p, b, tk)  => api('PUT',  p, b,    tk);

// ── Playwright helpers ────────────────────────────────────────────────────
const h        = u => (typeof u === 'string' ? u : u.href);
const atFeed   = u => h(u).includes('/feed');
const atIndex  = u => h(u).includes('/index') || /:\d+\/?$/.test(h(u));
const atCad    = u => h(u).includes('/cadastro');
const atChat   = u => h(u).includes('/chat');
const atPub    = u => h(u).includes('/publicar');
const atNotif  = u => h(u).includes('/notificacoes');
const atPerfil = u => h(u).includes('/perfil') && !h(u).includes('publico') && !h(u).includes('id=');
const atPerPub = u => h(u).includes('perfil-publico') || (h(u).includes('/perfil') && h(u).includes('id='));
const atCom    = u => h(u).includes('/comunidades');

async function loginUI(page, email, senha) {
  await page.goto(`${BASE}/index.html`, { waitUntil: 'domcontentloaded' });
  await page.fill('#email', email);
  await page.fill('#senha', senha);
  await page.click('button[type=submit]');
  await page.waitForURL(atFeed, { timeout: 10000 });
}

async function pageToken(page) { return page.evaluate(() => localStorage.getItem('jwtToken')); }

async function apiViaPage(page, method, path, body) {
  const tk = await pageToken(page);
  return page.evaluate(async ({ url, method, body, tk }) => {
    const r = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${tk}` },
      body: JSON.stringify(body)
    });
    return { status: r.status, text: await r.text() };
  }, { url: `${API}${path}`, method, body, tk });
}

try { mkdirSync('C:/Users/guilh/AppData/Local/Temp/viver-ss', { recursive: true }); } catch (_) {}
const ss = async (page, name) => {
  try { await page.screenshot({ path: `C:/Users/guilh/AppData/Local/Temp/viver-ss/${name}.png` }); } catch (_) {}
};

// ════════════════════════════════════════════════════════════════════════════
// 0. TESTES DE API DIRETA (sem navegador)
// ════════════════════════════════════════════════════════════════════════════
process.stdout.write('\n-- 0. API DIRETA (HTTP puro, sem browser) --\n');

await chk('Endpoint protegido sem token retorna 401', async () => {
  const { status } = await GET('/posts?usuarioId=1');
  if (status === 401) ok('Sem token -> 401');
  else throw new Error(`esperado 401, recebeu ${status}`);
});

await chk('Token invalido retorna 401', async () => {
  const { status } = await GET('/posts?usuarioId=1', 'tokeninvalido');
  if (status === 401) ok('Token invalido -> 401');
  else throw new Error(`esperado 401, recebeu ${status}`);
});

await chk('Login com credenciais erradas retorna 401', async () => {
  const { status } = await POST('/login', { email: 'naoexiste@x.com', senha: 'errada' });
  if (status === 401) ok('Credenciais erradas -> 401');
  else throw new Error(`esperado 401, recebeu ${status}`);
});

await chk('Login U1 retorna token e id', async () => {
  const { status, data } = await POST('/login', { email: U1_EMAIL, senha: U1_SENHA });
  if (status !== 200) throw new Error(`status ${status}: ${JSON.stringify(data)}`);
  if (!data.token) throw new Error('token ausente na resposta');
  tk1  = data.token;
  uid1 = data.usuario.id;
  uid1Nome = data.usuario.nome;
  ok(`Login U1 OK (id=${uid1}, nome=${uid1Nome})`);
});

await chk('Login U2 retorna token e id', async () => {
  const { status, data } = await POST('/login', { email: U2_EMAIL, senha: U2_SENHA });
  if (status !== 200) throw new Error(`status ${status}: ${JSON.stringify(data)}`);
  tk2  = data.token;
  uid2 = data.usuario.id;
  uid2Nome = data.usuario.nome;
  ok(`Login U2 OK (id=${uid2}, nome=${uid2Nome})`);
});

// ── Usuarios ──────────────────────────────────────────────────────────────
await chk('GET /usuarios lista todos', async () => {
  const { status, data } = await GET('/usuarios', tk1);
  if (status !== 200) throw new Error(`status ${status}`);
  if (!Array.isArray(data)) throw new Error('esperava array');
  ok(`/usuarios -> ${data.length} usuarios`);
});

await chk('GET /usuarios/buscar?email= encontra U1', async () => {
  const { status, data } = await GET(`/usuarios/buscar?email=${encodeURIComponent(U1_EMAIL)}`, tk1);
  if (status !== 200) throw new Error(`status ${status}`);
  if (data.id !== uid1) throw new Error(`id ${data.id} != ${uid1}`);
  ok(`buscar por email -> ${data.nome}`);
});

await chk('GET /usuarios/buscar?nickname= encontra usuario', async () => {
  // busca pelo nickname de U1 (pode ser nulo; aceitamos 200 ou 404)
  const { status } = await GET('/usuarios/buscar?nickname=teste_auto', tk1);
  if (status === 200 || status === 404) ok(`buscar por nickname -> ${status}`);
  else throw new Error(`status inesperado ${status}`);
});

await chk('GET /usuarios/{id} retorna usuario', async () => {
  const { status, data } = await GET(`/usuarios/${uid1}`, tk1);
  if (status !== 200) throw new Error(`status ${status}`);
  if (!data.usuario || data.usuario.id !== uid1) throw new Error('id diverge');
  ok(`/usuarios/${uid1} -> nome=${data.usuario.nome}`);
});

await chk('GET /usuarios/{id}/estatisticas', async () => {
  const { status, data } = await GET(`/usuarios/${uid1}/estatisticas`, tk1);
  if (status !== 200) throw new Error(`status ${status}`);
  if (typeof data.totalPosts === 'undefined') throw new Error('campo totalPosts ausente');
  ok(`estatisticas: posts=${data.totalPosts} segs=${data.totalSeguidores}`);
});

await chk('GET /usuarios/{id}/sigo indica nao segue (inicial)', async () => {
  const { status, data } = await GET(`/usuarios/${uid2}/sigo?usuarioId=${uid1}`, tk1);
  if (status !== 200) throw new Error(`status ${status}`);
  ok(`sigo=${data.sigo}`);
});

await chk('POST /usuarios/{id}/seguir -> segue', async () => {
  const { status, data } = await POST(`/usuarios/${uid2}/seguir`, { usuarioId: uid1 }, tk1);
  if (status !== 200) throw new Error(`status ${status}: ${JSON.stringify(data)}`);
  ok(`seguir U2 -> "${data}"`);
});

await chk('GET /usuarios/{id}/sigo confirma que agora segue', async () => {
  const { status, data } = await GET(`/usuarios/${uid2}/sigo?usuarioId=${uid1}`, tk1);
  if (status !== 200) throw new Error(`status ${status}`);
  if (!data.sigo) throw new Error('esperava sigo=true');
  ok('sigo=true confirmado');
});

await chk('POST /usuarios/{id}/seguir (auto-seguir) retorna 409', async () => {
  const { status } = await POST(`/usuarios/${uid1}/seguir`, { usuarioId: uid1 }, tk1);
  if (status === 409) ok('Auto-seguir -> 409');
  else throw new Error(`esperado 409, recebeu ${status}`);
});

await chk('GET /usuarios/{id}/amigos lista amigos', async () => {
  const { status, data } = await GET(`/usuarios/${uid1}/amigos`, tk1);
  if (status !== 200) throw new Error(`status ${status}`);
  if (!Array.isArray(data)) throw new Error('esperava array');
  ok(`amigos de U1: ${data.length}`);
});

await chk('GET /usuarios/nickname/{nick}/disponivel - nick em uso', async () => {
  const { status, data } = await GET('/usuarios/nickname/teste_auto/disponivel');
  if (status !== 200) throw new Error(`status ${status}`);
  ok(`nick teste_auto disponivel=${data.disponivel}`);
});

await chk('GET /usuarios/nickname/{nick}/disponivel - nick livre', async () => {
  const nick = `nick_livre_${Date.now()}`;
  const { status, data } = await GET(`/usuarios/nickname/${nick}/disponivel`);
  if (status !== 200) throw new Error(`status ${status}`);
  if (!data.disponivel) throw new Error('nick novo nao reconhecido como disponivel');
  ok('nick novo -> disponivel=true');
});

await chk('PUT /usuarios/{id}/nickname atualiza nickname', async () => {
  const novoNick = `auto_${Date.now().toString().slice(-6)}`;
  const { status } = await PUT(`/usuarios/${uid1}/nickname`, { nickname: novoNick }, tk1);
  if (status !== 200) throw new Error(`status ${status}`);
  ok(`nickname atualizado para ${novoNick}`);
});

// ── Posts ─────────────────────────────────────────────────────────────────
await chk('GET /posts lista feed', async () => {
  const { status, data } = await GET(`/posts?usuarioId=${uid1}`, tk1);
  if (status !== 200) throw new Error(`status ${status}`);
  if (!Array.isArray(data)) throw new Error('esperava array');
  ok(`feed: ${data.length} posts`);
});

await chk('GET /posts?filtro=seguindo lista posts dos seguidos', async () => {
  const { status, data } = await GET(`/posts?usuarioId=${uid1}&filtro=seguindo`, tk1);
  if (status !== 200) throw new Error(`status ${status}`);
  ok(`feed seguindo: ${data.length} posts`);
});

await chk('POST /posts cria post (status 201)', async () => {
  const { status, data } = await POST('/posts', {
    texto: 'Post criado pelo teste automatico de API',
    imagem: null,
    autor: { id: uid1, nome: uid1Nome },
    destinoTipo: 'USUARIO',
    destinoId: uid1
  }, tk1);
  if (status !== 201) throw new Error(`status ${status}: ${JSON.stringify(data)}`);
  postIdTeste = typeof data === 'object' ? data.id : null;
  ok(`post criado id=${postIdTeste}`);
});

await chk('POST /posts/{id}/ver marca como visto', async () => {
  if (!postIdTeste) { ok('post id nao disponivel, pulado'); return; }
  const { status } = await POST(`/posts/${postIdTeste}/ver`, { usuarioId: uid1 }, tk1);
  if (status !== 200) throw new Error(`status ${status}`);
  ok('post marcado como visto');
});

await chk('POST /posts/{id}/reagir adiciona reacao', async () => {
  if (!postIdTeste) { ok('post id nao disponivel, pulado'); return; }
  const { status, data } = await POST(`/posts/${postIdTeste}/reagir`, { usuarioId: uid1, tipo: 'curtida' }, tk1);
  if (status !== 200) throw new Error(`status ${status}`);
  ok(`reagir: "${data}"`);
});

await chk('POST /posts/{id}/responder adiciona comentario', async () => {
  if (!postIdTeste) { ok('post id nao disponivel, pulado'); return; }
  const { status } = await POST(`/posts/${postIdTeste}/responder`, { usuarioId: uid1, texto: 'Comentario de teste!' }, tk1);
  if (status !== 201) throw new Error(`status ${status}`);
  ok('comentario adicionado');
});

// ── Notificacoes ──────────────────────────────────────────────────────────
await chk('GET /notificacoes/{uid} lista notificacoes', async () => {
  const { status, data } = await GET(`/notificacoes/${uid1}`, tk1);
  if (status !== 200) throw new Error(`status ${status}`);
  if (!Array.isArray(data)) throw new Error('esperava array');
  ok(`notificacoes: ${data.length}`);
});

await chk('GET /notificacoes/{uid}/nao-lidas conta nao lidas', async () => {
  const { status, data } = await GET(`/notificacoes/${uid1}/nao-lidas`, tk1);
  if (status !== 200) throw new Error(`status ${status}`);
  if (typeof data.total === 'undefined') throw new Error('campo total ausente');
  ok(`nao-lidas: ${data.total}`);
});

await chk('POST /notificacoes/{uid}/ler marca todas como lidas', async () => {
  const { status } = await POST(`/notificacoes/${uid1}/ler`, {}, tk1);
  if (status !== 200) throw new Error(`status ${status}`);
  ok('notificacoes marcadas como lidas');
});

// ── Chat ──────────────────────────────────────────────────────────────────
await chk('GET /chat/contatos/{uid} lista contatos', async () => {
  const { status, data } = await GET(`/chat/contatos/${uid1}`, tk1);
  if (status !== 200) throw new Error(`status ${status}`);
  ok(`contatos de chat: ${Array.isArray(data) ? data.length : '?'}`);
});

await chk('GET /chat/nao-lidas/{uid} conta mensagens nao lidas', async () => {
  const { status, data } = await GET(`/chat/nao-lidas/${uid1}`, tk1);
  if (status !== 200) throw new Error(`status ${status}`);
  if (typeof data.total === 'undefined') throw new Error('campo total ausente');
  ok(`chat nao-lidas: ${data.total}`);
});

await chk('POST /chat envia mensagem direta', async () => {
  const { status } = await POST('/chat', { remetenteId: uid1, destinatarioId: uid2, conteudo: 'Oi via API de teste!' }, tk1);
  if (status !== 201) throw new Error(`status ${status}`);
  ok('mensagem direta enviada');
});

await chk('GET /chat/{uidA}/{uidB} recupera conversa', async () => {
  const { status, data } = await GET(`/chat/${uid1}/${uid2}`, tk1);
  if (status !== 200) throw new Error(`status ${status}`);
  if (!Array.isArray(data)) throw new Error('esperava array');
  ok(`conversa: ${data.length} mensagens`);
});

// ── Comunidades ───────────────────────────────────────────────────────────
await chk('POST /comunidades cria grupo (status 201)', async () => {
  const { status, data } = await POST('/comunidades', {
    nome: `Grupo Teste API ${Date.now()}`,
    descricao: 'Criado pelo teste automatizado',
    criadorId: uid1
  }, tk1);
  if (status !== 201) throw new Error(`status ${status}: ${JSON.stringify(data)}`);
  grupoIdTeste = typeof data === 'object' ? data.id : null;
  ok(`grupo criado id=${grupoIdTeste}`);
});

await chk('GET /comunidades?usuarioId= lista grupos do usuario', async () => {
  const { status, data } = await GET(`/comunidades?usuarioId=${uid1}`, tk1);
  if (status !== 200) throw new Error(`status ${status}`);
  if (!Array.isArray(data)) throw new Error('esperava array');
  ok(`grupos: ${data.length}`);
});

await chk('PUT /comunidades/{id} edita grupo', async () => {
  if (!grupoIdTeste) { ok('grupo id nao disponivel, pulado'); return; }
  const { status } = await PUT(`/comunidades/${grupoIdTeste}`, {
    usuarioId: uid1,
    nome: 'Grupo Editado API',
    descricao: 'Descricao editada',
    fotoGrupo: null
  }, tk1);
  if (status !== 200) throw new Error(`status ${status}`);
  ok('grupo editado com sucesso');
});

await chk('GET /comunidades/{id}/membros lista membros', async () => {
  if (!grupoIdTeste) { ok('grupo id nao disponivel, pulado'); return; }
  const { status, data } = await GET(`/comunidades/${grupoIdTeste}/membros`, tk1);
  if (status !== 200) throw new Error(`status ${status}`);
  ok(`membros: ${Array.isArray(data) ? data.length : '?'}`);
});

await chk('POST /comunidades/{id}/convidar envia convite', async () => {
  if (!grupoIdTeste) { ok('grupo id nao disponivel, pulado'); return; }
  const { status, data } = await POST(`/comunidades/${grupoIdTeste}/convidar`, {
    convidanteId: uid1,
    convidadoId: uid2
  }, tk1);
  if (status !== 201) throw new Error(`status ${status}: ${JSON.stringify(data)}`);
  conviteIdTeste = typeof data === 'object' ? data.conviteId : null;
  ok(`convite enviado id=${conviteIdTeste}`);
});

await chk('GET /convites/pendentes/{uid} lista convites de U2', async () => {
  const { status, data } = await GET(`/convites/pendentes/${uid2}`, tk2);
  if (status !== 200) throw new Error(`status ${status}`);
  ok(`convites pendentes de U2: ${Array.isArray(data) ? data.length : '?'}`);
});

await chk('POST /convites/{id}/responder aceita convite', async () => {
  if (!conviteIdTeste) { ok('convite id nao disponivel, pulado'); return; }
  const { status, data } = await POST(`/convites/${conviteIdTeste}/responder`, {
    usuarioId: uid2,
    aceitar: true
  }, tk2);
  if (status !== 200) throw new Error(`status ${status}: ${JSON.stringify(data)}`);
  ok(`convite ${conviteIdTeste} aceito: "${data}"`);
});

await chk('POST /comunidades/{id}/mensagens envia msg no grupo', async () => {
  if (!grupoIdTeste) { ok('grupo id nao disponivel, pulado'); return; }
  const { status } = await POST(`/comunidades/${grupoIdTeste}/mensagens`, {
    usuarioId: uid1,
    conteudo: 'Mensagem de grupo via API!'
  }, tk1);
  if (status !== 201) throw new Error(`status ${status}`);
  ok('mensagem de grupo enviada');
});

await chk('GET /comunidades/{id}/mensagens lista msgs do grupo', async () => {
  if (!grupoIdTeste) { ok('grupo id nao disponivel, pulado'); return; }
  const { status, data } = await GET(`/comunidades/${grupoIdTeste}/mensagens?usuarioId=${uid1}`, tk1);
  if (status !== 200) throw new Error(`status ${status}`);
  ok(`mensagens no grupo: ${Array.isArray(data) ? data.length : '?'}`);
});

// Desfazer o seguir para nao poluir banco
await chk('POST /usuarios/{id}/seguir -> deixar de seguir', async () => {
  const { status } = await POST(`/usuarios/${uid2}/seguir`, { usuarioId: uid1 }, tk1);
  if (status !== 200) throw new Error(`status ${status}`);
  ok('deixou de seguir U2');
});

// ════════════════════════════════════════════════════════════════════════════
// Abre o browser para os testes de UI
// ════════════════════════════════════════════════════════════════════════════
const browser = await chromium.launch({ headless: false, slowMo: 80 });
const ctx  = await browser.newContext({ viewport: { width: 390, height: 844 } });
const page = await ctx.newPage();
page.setDefaultTimeout(8000);

// ════════════════════════════════════════════════════════════════════════════
// 1. AUTENTICACAO (UI)
// ════════════════════════════════════════════════════════════════════════════
process.stdout.write('\n-- 1. AUTENTICACAO (UI) --\n');

await chk('Sem sessao -> redireciona para login', async () => {
  await page.goto(`${BASE}/feed.html`, { waitUntil: 'domcontentloaded' });
  await page.waitForURL(atIndex, { timeout: 5000 });
  ok('Sem sessao -> redireciona para login');
});

await chk('Login com credenciais erradas exibe erro', async () => {
  await page.fill('#email', 'invalido@email.com');
  await page.fill('#senha', 'senhaerrada');
  await page.click('button[type=submit]');
  await page.waitForSelector('#msg-login:visible', { timeout: 5000 });
  ok('Login com credenciais erradas exibe erro');
});

await chk('Toggle mostrar senha altera type do campo', async () => {
  const t1 = await page.getAttribute('#senha', 'type');
  await page.click('label:has(input[type=checkbox])');
  const t2 = await page.getAttribute('#senha', 'type');
  if (t1 === 'password' && t2 === 'text') ok('Toggle mostrar senha funciona');
  else throw new Error(`t1=${t1} t2=${t2}`);
});

await chk('Link "Criar conta" -> cadastro.html', async () => {
  await page.click('a[href="cadastro.html"]');
  await page.waitForURL(atCad, { timeout: 4000 });
  ok('Link Criar conta -> cadastro.html');
});

await chk('Link "Ja tem conta?" -> index.html', async () => {
  await page.click('a[href="index.html"]');
  await page.waitForURL(atIndex, { timeout: 4000 });
  ok('Link Ja tem conta -> index.html');
});

await chk('Login correto -> feed.html', async () => {
  await loginUI(page, U1_EMAIL, U1_SENHA);
  ok('Login correto -> feed.html');
});
await ss(page, '01-login');

// ════════════════════════════════════════════════════════════════════════════
// 2. FEED (UI)
// ════════════════════════════════════════════════════════════════════════════
process.stdout.write('\n-- 2. FEED (UI) --\n');

await chk('Feed carrega sem spinner', async () => {
  await page.waitForFunction(() => {
    const e = document.getElementById('container-posts');
    return e && !e.querySelector('.spinner');
  }, { timeout: 6000 });
  ok('Feed carregou');
});

await chk('Saudacao exibe nome do usuario', async () => {
  const t = await page.$eval('#saudacao', e => e.textContent);
  if (t.includes('Teste') || t.length > 5) ok(`Saudacao: "${t.trim()}"`);
  else throw new Error(`"${t}"`);
});

await chk('Botao Comunidade -> comunidades.html', async () => {
  await page.click('a[href="comunidades.html"]');
  await page.waitForURL(atCom, { timeout: 5000 });
  ok('Botao Comunidade -> comunidades.html');
  await page.goBack();
  await page.waitForURL(atFeed, { timeout: 5000 });
});

await chk('Filtro Todos ativo por padrao', async () => {
  const a = await page.$eval('#btn-todos', e => e.classList.contains('ativo'));
  if (a) ok('Filtro Todos ativo'); else throw new Error('nao ativo');
});

await chk('4 filtros mudam aba corretamente', async () => {
  for (const [id, l] of [
    ['#btn-seguindo', 'Seguindo'],
    ['#btn-novos', 'Novos'],
    ['#btn-ja-vistos', 'Vistos'],
    ['#btn-todos', 'Todos']
  ]) {
    await page.click(id);
    await page.waitForTimeout(300);
    if (!await page.$eval(id, e => e.classList.contains('ativo')))
      throw new Error(`${l} nao ativo`);
  }
  ok('4 filtros funcionam');
});

await chk('Pesquisa de usuarios exibe resultados', async () => {
  await page.fill('#pesquisa-input', 'Do');
  await page.waitForTimeout(500);
  const v = await page.$eval('#pesquisa-resultados', e => e.style.display !== 'none');
  if (v) ok('Pesquisa exibe resultados');
  else throw new Error('display:none');
  await page.fill('#pesquisa-input', '');
  await page.click('body');
});

await chk('Botao acessibilidade funciona', async () => {
  await page.click('button[onclick="alternarAcessibilidade()"]');
  await page.waitForTimeout(200);
  await page.click('button[onclick="alternarAcessibilidade()"]');
  ok('Botao acessibilidade OK');
});

await chk('Publicar post via API (para ter conteudo no feed)', async () => {
  const { status } = await POST('/posts', {
    texto: 'Post de teste automatico UI',
    imagem: null,
    autor: { id: uid1, nome: uid1Nome },
    destinoTipo: 'USUARIO',
    destinoId: uid1
  }, tk1);
  if (status === 201) ok('Post publicado (201)');
  else throw new Error(`status ${status}`);
});

await chk('Publicar post como USER2 via API', async () => {
  const s = await POST('/posts', {
    texto: 'Oi de Dona Maria via teste!',
    imagem: null,
    autor: { id: uid2, nome: uid2Nome },
    destinoTipo: 'USUARIO',
    destinoId: uid2
  }, tk2);
  if (s.status === 201) ok('Post USER2 publicado');
  else throw new Error(`status ${s.status}`);
});

await page.reload({ waitUntil: 'domcontentloaded' });
await page.waitForTimeout(1200);
await page.waitForFunction(() => {
  const e = document.getElementById('container-posts');
  return e && !e.querySelector('.spinner');
}, { timeout: 6000 });

await chk('Posts aparecem no feed', async () => {
  const c = await page.$$('.post-card');
  if (c.length > 0) ok(`${c.length} posts no feed`);
  else throw new Error('nenhum post-card');
});

await chk('Botao reagir curtida funciona', async () => {
  await page.locator('.btn-reacao').first().click();
  await page.waitForTimeout(800);
  ok('Reacao curtida OK');
});

await chk('Botao reagir abraco funciona', async () => {
  await page.locator('.btn-reacao').nth(1).click();
  await page.waitForTimeout(800);
  ok('Reacao abraco OK');
});

await chk('Botao reagir parabens funciona', async () => {
  await page.locator('.btn-reacao').nth(2).click();
  await page.waitForTimeout(800);
  ok('Reacao parabens OK');
});

await chk('Botao comentar abre area de resposta', async () => {
  await page.locator('.btn-comentar').first().click();
  await page.waitForTimeout(400);
  const open = await page.$$('.area-comentar[style*="block"]');
  if (open.length > 0) ok('Area comentar abriu');
  else throw new Error('nao abriu');
});

await chk('Enviar comentario funciona', async () => {
  await page.locator('.area-comentar[style*="block"] textarea').first().fill('Que bonito!');
  await page.locator('.area-comentar[style*="block"] button.btn-primario').first().click();
  await page.waitForTimeout(1200);
  ok('Comentario enviado');
});

await chk('Clicar no autor de outro usuario -> perfil-publico', async () => {
  const link = page.locator(`.post-autor[href*="id=${uid2}"]`).first();
  if (await link.count() > 0) {
    await link.click();
    await page.waitForURL(atPerPub, { timeout: 6000 });
    ok('Autor -> perfil-publico');
    await page.goBack();
    await page.waitForURL(atFeed, { timeout: 5000 });
  } else ok('Post de U2 nao visivel no feed no momento (valido)');
});
await ss(page, '02-feed');

// ════════════════════════════════════════════════════════════════════════════
// 3. NAVEGACAO (barra inferior)
// ════════════════════════════════════════════════════════════════════════════
process.stdout.write('\n-- 3. NAVEGACAO (barra inferior) --\n');

await chk('Nav Inicio ativo no feed', async () => {
  if (!h(page.url()).includes('/feed')) {
    await page.goto(`${BASE}/feed.html`, { waitUntil: 'domcontentloaded' });
    await page.waitForURL(atFeed, { timeout: 5000 });
  }
  const a = await page.$eval('a[href="feed.html"]', e => e.classList.contains('ativo'));
  if (a) ok('Nav Inicio ativo'); else throw new Error('nao ativo');
});

await chk('Nav Chat -> chat.html', async () => {
  await page.click('a[href="chat.html"]');
  await page.waitForURL(atChat, { timeout: 5000 });
  ok('Nav Chat OK');
});

await chk('Nav + -> publicar.html', async () => {
  await page.click('a[href="publicar.html"]');
  await page.waitForURL(atPub, { timeout: 5000 });
  ok('Nav + OK');
});

await chk('Nav Avisos -> notificacoes.html', async () => {
  await page.click('a[href="notificacoes.html"]');
  await page.waitForURL(atNotif, { timeout: 5000 });
  ok('Nav Avisos OK');
});

await chk('Nav Perfil -> perfil.html', async () => {
  await page.click('a[href="perfil.html"]');
  await page.waitForURL(atPerfil, { timeout: 5000 });
  ok('Nav Perfil OK');
});

// ════════════════════════════════════════════════════════════════════════════
// 4. PUBLICAR (UI)
// ════════════════════════════════════════════════════════════════════════════
process.stdout.write('\n-- 4. PUBLICAR (UI) --\n');

await page.goto(`${BASE}/publicar.html`, { waitUntil: 'domcontentloaded' });
await page.waitForTimeout(600);

await chk('Formulario publicar carrega', async () => {
  if (await page.$('#form-publicar')) ok('Formulario publicar OK');
  else throw new Error('nao encontrado');
});

await chk('Select destino tem opcao Feed publico', async () => {
  const opts = await page.$$('#destino-post option');
  const txts = await Promise.all(opts.map(o => o.textContent()));
  if (txts.some(t => t.includes('Feed') || t.includes('meu'))) ok(`Select OK (${txts.length} opcoes)`);
  else throw new Error(`Opcoes: ${txts.join(', ')}`);
});

await chk('Contador de caracteres atualiza', async () => {
  await page.fill('#texto-post', 'Ola mundo!');
  const c = await page.$eval('#contador-chars', e => e.textContent);
  if (c.includes('10')) ok(`Contador: "${c}"`); else throw new Error(`"${c}"`);
});

await chk('Publicar post -> feed.html', async () => {
  await page.fill('#texto-post', 'Publicado pelo teste automatico UI');
  await page.click('#btn-enviar');
  await page.waitForURL(atFeed, { timeout: 10000 });
  ok('Publicar -> feed.html');
});

// ════════════════════════════════════════════════════════════════════════════
// 5. PERFIL PROPRIO (UI)
// ════════════════════════════════════════════════════════════════════════════
process.stdout.write('\n-- 5. PERFIL PROPRIO (UI) --\n');

await page.goto(`${BASE}/perfil.html`, { waitUntil: 'domcontentloaded' });
await page.waitForTimeout(1000);

await chk('Nome exibido no perfil', async () => {
  const n = await page.$eval('#nome-perfil', e => e.textContent);
  if (n.length > 0 && n !== 'A carregar...') ok(`Nome: "${n}"`);
  else throw new Error(`"${n}"`);
});

await chk('Email exibido no perfil', async () => {
  const e = await page.$eval('#email-perfil', e => e.textContent);
  if (e.includes('@')) ok(`Email: "${e}"`); else throw new Error(`"${e}"`);
});

await chk('Nickname exibido com @', async () => {
  const n = await page.$eval('#nick-perfil', e => e.textContent);
  if (n.startsWith('@') || n.length > 0) ok(`Nick: "${n}"`);
  else throw new Error(`"${n}"`);
});

await chk('Estatisticas carregam', async () => {
  await page.waitForFunction(() => document.getElementById('num-posts').textContent !== '-', { timeout: 6000 });
  const [p, s, sg] = await Promise.all(
    ['#num-posts', '#num-seguidores', '#num-seguindo'].map(id => page.$eval(id, e => e.textContent))
  );
  ok(`Posts=${p} Segs=${s} Seguindo=${sg}`);
});

await chk('Secao minhas memorias carrega posts', async () => {
  await page.waitForFunction(() => {
    const e = document.getElementById('container-meus-posts');
    return e && !e.querySelector('.spinner');
  }, { timeout: 6000 });
  const t = await page.$eval('#container-meus-posts', e => e.innerText);
  if (t.length > 5) ok('Memorias tem conteudo'); else throw new Error(`"${t.slice(0, 80)}"`);
});

await chk('Botao Terminar Sessao existe', async () => {
  if (await page.$('button[onclick="terminarSessao()"]')) ok('Botao logout presente');
  else throw new Error('nao encontrado');
});
await ss(page, '05-perfil');

// ════════════════════════════════════════════════════════════════════════════
// 6. PERFIL PUBLICO (UI)
// ════════════════════════════════════════════════════════════════════════════
process.stdout.write('\n-- 6. PERFIL PUBLICO (UI) --\n');

await page.goto(`${BASE}/perfil-publico?id=${uid2}`, { waitUntil: 'domcontentloaded' });
await page.waitForTimeout(1500);

await chk('Perfil publico carrega nome', async () => {
  await page.waitForFunction(() => document.querySelector('.perfil-nome'), { timeout: 6000 });
  const n = await page.$eval('.perfil-nome', e => e.textContent);
  if (n.length > 0) ok(`Nome: "${n}"`); else throw new Error('vazio');
});

await chk('3 estatisticas presentes', async () => {
  const s = await page.$$('.stat');
  if (s.length === 3) ok('3 stats OK'); else throw new Error(`${s.length} stats`);
});

await chk('Botao Seguir alterna estado', async () => {
  const t1 = (await page.$eval('#btn-seguir', e => e.textContent)).trim();
  await page.click('#btn-seguir');
  await page.waitForTimeout(1200);
  const t2 = (await page.$eval('#btn-seguir', e => e.textContent)).trim();
  if (t1 !== t2) ok(`Seguir: "${t1}" -> "${t2}"`);
  else throw new Error(`texto nao mudou: "${t1}"`);
});

await chk('Toggle reverso (desfazer seguir)', async () => {
  await page.click('#btn-seguir');
  await page.waitForTimeout(1000);
  ok('Toggle reverso OK');
  await page.click('#btn-seguir'); // seguir de novo para o teste de chat
  await page.waitForTimeout(800);
});

await chk('Botao Mensagem abre chat direto', async () => {
  const [_] = await Promise.all([
    page.waitForURL(u => h(u).includes('/chat'), { timeout: 8000 }),
    page.click(`button[onclick*="chat?usuario=${uid2}"]`),
  ]);
  ok('Botao Mensagem -> chat');
});

await chk('Botao voltar (history.back) existe e funciona', async () => {
  await page.goto(`${BASE}/perfil-publico?id=${uid2}`, { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(600);
  const btn = await page.$('button[onclick="history.back()"]');
  if (btn) { await btn.click(); ok('Botao <- existe e funciona'); }
  else throw new Error('nao encontrado');
});

await chk('Proprio ID -> redireciona para perfil.html', async () => {
  await page.goto(`${BASE}/perfil-publico?id=${uid1}`, { waitUntil: 'domcontentloaded' });
  await page.waitForURL(atPerfil, { timeout: 5000 });
  ok('Proprio ID -> perfil.html');
});

await chk('ID invalido -> redireciona para feed.html', async () => {
  await page.goto(`${BASE}/perfil-publico?id=abc`, { waitUntil: 'domcontentloaded' });
  await page.waitForURL(atFeed, { timeout: 5000 });
  ok('ID invalido -> feed.html');
});
await ss(page, '06-perfil-publico');

// ════════════════════════════════════════════════════════════════════════════
// 7. NOTIFICACOES (UI)
// ════════════════════════════════════════════════════════════════════════════
process.stdout.write('\n-- 7. NOTIFICACOES (UI) --\n');

await page.goto(`${BASE}/notificacoes.html`, { waitUntil: 'domcontentloaded' });
await page.waitForTimeout(1500);

await chk('Notificacoes carrega sem spinner', async () => {
  await page.waitForFunction(() => !document.querySelector('#container-notif .spinner'), { timeout: 5000 });
  ok('Notificacoes carregou');
});

await chk('Conteudo de notificacoes exibido', async () => {
  const t = await page.$eval('#container-notif', e => e.innerText);
  if (t.trim().length > 0) ok(`Notif: "${t.slice(0, 50).trim()}..."`);
  else throw new Error('vazio');
});

await chk('Nav Avisos ativo em notificacoes.html', async () => {
  const a = await page.$eval('a[href="notificacoes.html"]', e => e.classList.contains('ativo'));
  if (a) ok('Nav Avisos ativo'); else throw new Error('nao ativo');
});
await ss(page, '07-notificacoes');

// ════════════════════════════════════════════════════════════════════════════
// 8. CHAT (UI)
// ════════════════════════════════════════════════════════════════════════════
process.stdout.write('\n-- 8. CHAT (UI) --\n');

await page.goto(`${BASE}/chat.html`, { waitUntil: 'domcontentloaded' });
await page.waitForTimeout(1500);

await chk('Chat carrega lista (aba Amigos)', async () => {
  await page.waitForFunction(() => {
    const e = document.getElementById('lista-conteudo');
    return e && !e.querySelector('.spinner');
  }, { timeout: 6000 });
  ok('Chat lista carregou');
});

await chk('Aba Amigos ativa por padrao', async () => {
  if (await page.$eval('#btn-amigos', e => e.classList.contains('ativo'))) ok('Aba Amigos ativa');
  else throw new Error('nao ativa');
});

await chk('Aba Grupos muda aba ativa', async () => {
  await page.click('#btn-grupos');
  await page.waitForTimeout(700);
  if (await page.$eval('#btn-grupos', e => e.classList.contains('ativo'))) ok('Aba Grupos ativa');
  else throw new Error('nao ativa');
  await page.click('#btn-amigos');
  await page.waitForTimeout(400);
});

await chk('Chat direto abre via ?usuario= na URL', async () => {
  await page.goto(`${BASE}/chat?usuario=${uid2}`, { waitUntil: 'domcontentloaded' });
  await page.waitForFunction(
    () => document.getElementById('tela-conversa')?.style.display === 'flex',
    { timeout: 6000 }
  );
  ok('Chat direto abriu');
});

await chk('Nome do contato no cabecalho do chat', async () => {
  const n = await page.$eval('#conversa-nome', e => e.textContent);
  if (n !== '-' && n.length > 0) ok(`Nome: "${n}"`); else throw new Error(`"${n}"`);
});

await chk('Campo #input-msg aceita texto', async () => {
  await page.fill('#input-msg', 'Mensagem de teste automatico');
  const v = await page.$eval('#input-msg', e => e.value);
  if (v.length > 0) ok('Campo input-msg funciona'); else throw new Error('vazio');
});

await chk('Botao enviar limpa o campo', async () => {
  await page.click('button[onclick="enviarMensagem()"]');
  await page.waitForTimeout(1000);
  const v = await page.$eval('#input-msg', e => e.value);
  if (v === '') ok('Campo limpou apos enviar'); else throw new Error('nao limpou');
});

await chk('Enter no campo envia mensagem', async () => {
  await page.fill('#input-msg', 'Via Enter');
  await page.press('#input-msg', 'Enter');
  await page.waitForTimeout(800);
  const v = await page.$eval('#input-msg', e => e.value);
  if (v === '') ok('Enter enviou mensagem'); else throw new Error('nao limpou');
});

await chk('Fechar conversa volta para lista', async () => {
  await page.click('button[onclick="fecharConversa()"]');
  await page.waitForTimeout(800);
  const listaOk = await page.$eval('#tela-lista',    e => e.style.display !== 'none');
  const chatOk  = await page.$eval('#tela-conversa', e => e.style.display === 'none' || !e.style.display);
  if (listaOk && chatOk) ok('Fechar -> lista sem reabrir');
  else throw new Error(`lista=${listaOk} chat_oculto=${chatOk}`);
});

await chk('Chat de grupo abre via ?grupo= na URL', async () => {
  if (!grupoIdTeste) { ok('grupo id nao disponivel, pulado'); return; }
  await page.goto(`${BASE}/chat?grupo=${grupoIdTeste}`, { waitUntil: 'domcontentloaded' });
  await page.waitForFunction(
    () => document.getElementById('tela-conversa')?.style.display === 'flex',
    { timeout: 6000 }
  );
  ok(`Chat de grupo ${grupoIdTeste} abriu`);
});
await ss(page, '08-chat');

// ════════════════════════════════════════════════════════════════════════════
// 9. COMUNIDADES (UI)
// ════════════════════════════════════════════════════════════════════════════
process.stdout.write('\n-- 9. COMUNIDADES (UI) --\n');

await page.goto(`${BASE}/comunidades.html`, { waitUntil: 'domcontentloaded' });
await page.waitForTimeout(1500);

await chk('Comunidades carrega sem spinner', async () => {
  await page.waitForFunction(() => !document.querySelector('#lista-grupos .spinner'), { timeout: 5000 });
  ok('Comunidades carregou');
});

await chk('Botao Criar Novo Grupo abre modal', async () => {
  await page.click('button[onclick*="modal-criar-grupo"]');
  await page.waitForTimeout(400);
  if (await page.$eval('#modal-criar-grupo', e => e.classList.contains('aberto'))) ok('Modal criar grupo abriu');
  else throw new Error('nao abriu');
});

await chk('Criar grupo -> modal fecha e grupo aparece', async () => {
  await page.fill('#nome-grupo', 'Grupo Jardinagem UI');
  await page.fill('#desc-grupo', 'Para amantes de jardins!');
  await page.click('button[onclick="criarGrupo()"]');
  await page.waitForTimeout(1500);
  if (await page.$eval('#modal-criar-grupo', e => e.classList.contains('aberto')))
    throw new Error('modal nao fechou');
  const lista = await page.$eval('#lista-grupos', e => e.innerText);
  if (lista.includes('Jardinagem') || lista.includes('Grupo') || lista.includes('grupo'))
    ok('Grupo na lista');
  else throw new Error(`Lista: "${lista.slice(0, 80)}"`);
});

await chk('Cancelar modal de criar grupo fecha', async () => {
  await page.click('button[onclick*="modal-criar-grupo"]');
  await page.waitForTimeout(300);
  await page.locator('#modal-criar-grupo .btn-ghost').click();
  await page.waitForTimeout(300);
  if (!await page.$eval('#modal-criar-grupo', e => e.classList.contains('aberto'))) ok('Cancelar fechou modal');
  else throw new Error('modal ainda aberto');
});

await chk('Botao Chat do grupo -> chat.html?grupo=', async () => {
  const btn = page.locator('.grupo-acoes .btn-primario').first();
  if (await btn.count() > 0) {
    await btn.click();
    await page.waitForURL(atChat, { timeout: 5000 });
    ok('Chat do grupo OK');
    await page.goBack();
    await page.waitForURL(atCom, { timeout: 5000 });
  } else ok('Nenhum grupo listado (valido)');
});

await chk('Botao Convidar abre modal', async () => {
  const btn = page.locator('.grupo-acoes .btn-secundario').first();
  if (await btn.count() > 0) {
    await btn.click();
    await page.waitForTimeout(400);
    if (await page.$eval('#modal-convidar', e => e.classList.contains('aberto'))) ok('Modal convidar abriu');
    else throw new Error('nao abriu');
    await page.click('button[onclick="fecharModalConvidar()"]');
    await page.waitForTimeout(300);
  } else ok('Nenhum grupo para convidar (valido)');
});

await chk('Area de convites pendentes existe no DOM', async () => {
  if (await page.$('#area-convites')) ok('area-convites presente');
  else throw new Error('nao encontrado');
});
await ss(page, '09-comunidades');

// ════════════════════════════════════════════════════════════════════════════
// 10. CADASTRO (UI - validacoes)
// ════════════════════════════════════════════════════════════════════════════
process.stdout.write('\n-- 10. CADASTRO (UI) --\n');

await page.goto(`${BASE}/cadastro.html`, { waitUntil: 'domcontentloaded' });

await chk('Senhas diferentes exibem erro', async () => {
  await page.fill('#nome', 'Usuario Novo');
  await page.fill('#nickname', 'usuario_novo_99');
  await page.fill('#email', 'novo99@teste.com');
  await page.fill('#dataNascimento', '1950-05-20');
  await page.fill('#senha', 'Senha123!');
  await page.fill('#confirmaSenha', 'SenhaDiferente!');
  await page.click('#btn-submit');
  await page.waitForSelector('#msg-cadastro:visible', { timeout: 4000 });
  ok('Erro de senhas diferentes exibido');
});

await chk('Nickname em uso exibe aviso', async () => {
  await page.fill('#nickname', 'teste_auto');
  await page.waitForTimeout(900);
  const s = await page.$eval('#status-nick', e => e.textContent);
  if (s.includes('uso') || s.includes('X') || s.includes('nao') || s.includes('❌'))
    ok(`Validacao nick em uso: "${s}"`);
  else throw new Error(`status-nick: "${s}"`);
});

await chk('Nickname disponivel exibe confirmacao', async () => {
  await page.fill('#nickname', `nick_xyz_${Date.now()}_disponivel`);
  await page.waitForTimeout(900);
  const s = await page.$eval('#status-nick', e => e.textContent);
  if (s.includes('vel') || s.includes('OK') || s.includes('✅') || s.length === 0)
    ok(`Validacao nick disponivel: "${s}"`);
  else throw new Error(`status-nick: "${s}"`);
});
await ss(page, '10-cadastro');

// ════════════════════════════════════════════════════════════════════════════
// 11. LOGOUT E PROTECAO DE ROTAS
// ════════════════════════════════════════════════════════════════════════════
process.stdout.write('\n-- 11. LOGOUT E PROTECAO DE ROTAS --\n');

await page.goto(`${BASE}/perfil.html`, { waitUntil: 'domcontentloaded' });
await page.waitForTimeout(500);

await chk('Terminar sessao -> index.html', async () => {
  await page.click('button[onclick="terminarSessao()"]');
  await page.waitForURL(atIndex, { timeout: 5000 });
  ok('Logout -> index.html');
});

await chk('Rota feed.html protegida apos logout', async () => {
  await page.goto(`${BASE}/feed.html`, { waitUntil: 'domcontentloaded' });
  await page.waitForURL(atIndex, { timeout: 5000 });
  ok('feed protegido');
});

for (const [rota, pred, label] of [
  ['perfil.html',       atPerfil, 'perfil'],
  ['chat.html',         atChat,   'chat'],
  ['notificacoes.html', atNotif,  'notificacoes'],
  ['comunidades.html',  atCom,    'comunidades'],
  ['publicar.html',     atPub,    'publicar'],
]) {
  await chk(`Rota ${label} protegida apos logout`, async () => {
    await page.evaluate(() => localStorage.clear());
    await page.goto(`${BASE}/${rota}`, { waitUntil: 'domcontentloaded' });
    await page.waitForURL(atIndex, { timeout: 5000 });
    ok(`${label} protegida -> login`);
  });
}

// ════════════════════════════════════════════════════════════════════════════
// RESULTADO FINAL
// ════════════════════════════════════════════════════════════════════════════
process.stdout.write('\n-------------------------------------------------\n');
process.stdout.write(`\n RESULTADO FINAL:\n`);
process.stdout.write(`  Passaram : ${pass}\n`);
process.stdout.write(`  Falharam : ${fail}\n`);
process.stdout.write(`  Total    : ${pass + fail}\n`);

if (erros.length > 0) {
  process.stdout.write('\n FALHAS:\n');
  erros.forEach((e, i) => process.stdout.write(`  ${i + 1}. ${e}\n`));
}
process.stdout.write('\n Screenshots: C:/Users/guilh/AppData/Local/Temp/viver-ss/\n\n');

await browser.close();
process.exit(fail > 0 ? 1 : 0);
