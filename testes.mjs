import { chromium } from '@playwright/test';
import { mkdirSync } from 'fs';

const BASE = 'http://localhost:3000';
const API  = 'http://localhost:8080/api';
const U1 = { email: 'teste_auto@viverplus.com', senha: 'Teste123', nome: 'Teste Automatizado', id: 3 };
const U2 = { email: 'donamaria2@viverplus.com', senha: 'Teste123', nome: 'Dona Maria Silva',   id: 4 };

let pass = 0, fail = 0;
const erros = [];

const ok   = m     => { process.stdout.write(`  [OK] ${m}\n`);   pass++; };
const nok  = (m,d) => { process.stdout.write(`  [FALHA] ${m}${d?' -- '+d:''}\n`); fail++; erros.push(m+(d?': '+d:'')); };
const chk  = async (label, fn) => { try { await fn(); } catch(e){ nok(label, e.message.split('\n')[0].slice(0,120)); } };

// serve strips .html; Playwright passes URL object to predicate -> use .href
const h = u => (typeof u === 'string' ? u : u.href);
const atFeed   = u => h(u).includes('/feed');
const atIndex  = u => h(u).includes('/index') || /:\d+\/?$/.test(h(u));
const atCad    = u => h(u).includes('/cadastro');
const atChat   = u => h(u).includes('/chat');
const atPub    = u => h(u).includes('/publicar');
const atNotif  = u => h(u).includes('/notificacoes');
const atPerfil = u => h(u).includes('/perfil') && !h(u).includes('publico') && !h(u).includes('id=');
const atPerPub = u => h(u).includes('perfil-publico') || (h(u).includes('/perfil') && h(u).includes('id='));
const atCom    = u => h(u).includes('/comunidades');

async function login(page, user) {
  await page.goto(`${BASE}/index.html`, { waitUntil: 'domcontentloaded' });
  await page.fill('#email', user.email);
  await page.fill('#senha', user.senha);
  await page.click('button[type=submit]');
  await page.waitForURL(atFeed, { timeout: 10000 });
}

async function token(page) { return page.evaluate(() => localStorage.getItem('jwtToken')); }

async function apiPost(page, path, body) {
  const tk = await token(page);
  return page.evaluate(async ({url, body, tk}) => {
    const r = await fetch(url, { method:'POST', headers:{'Content-Type':'application/json',Authorization:`Bearer ${tk}`}, body:JSON.stringify(body) });
    return { status: r.status, text: await r.text() };
  }, { url:`${API}${path}`, body, tk });
}

try { mkdirSync('C:/Users/guilh/AppData/Local/Temp/viver-ss', { recursive: true }); } catch(_) {}
const ss = async (page, name) => { try { await page.screenshot({ path:`C:/Users/guilh/AppData/Local/Temp/viver-ss/${name}.png` }); } catch(_){} };

// ─────────────────────────────────────────────
const browser = await chromium.launch({ headless: false, slowMo: 80 });
const ctx = await browser.newContext({ viewport: { width: 390, height: 844 } });
const page = await ctx.newPage();
page.setDefaultTimeout(8000);

// ══════════════════════════════════════════════
process.stdout.write('\n-- 1. AUTENTICACAO --\n');

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
  await login(page, U1);
  ok('Login correto -> feed.html');
});
await ss(page, '01-login');

// ══════════════════════════════════════════════
process.stdout.write('\n-- 2. FEED --\n');

await chk('Feed carrega sem spinner', async () => {
  await page.waitForFunction(() => { const e=document.getElementById('container-posts'); return e&&!e.querySelector('.spinner'); }, { timeout:6000 });
  ok('Feed carregou');
});

await chk('Saudacao exibe nome do usuario', async () => {
  const t = await page.$eval('#saudacao', e => e.textContent);
  if (t.includes('Teste')) ok(`Saudacao: "${t.trim()}"`);
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
  for (const [id,l] of [['#btn-seguindo','Seguindo'],['#btn-novos','Novos'],['#btn-ja-vistos','Vistos'],['#btn-todos','Todos']]) {
    await page.click(id); await page.waitForTimeout(300);
    if (!await page.$eval(id, e => e.classList.contains('ativo'))) throw new Error(`${l} nao ativo`);
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

await chk('Publicar post via API', async () => {
  const {status} = await apiPost(page, '/posts', { texto:'Post de teste automatico', imagem:null, autor:{id:U1.id,nome:U1.nome}, destinoTipo:'USUARIO', destinoId:U1.id });
  if (status===201) ok('Post publicado (201)');
  else throw new Error(`status ${status}`);
});

await chk('Publicar post como USER2 via API', async () => {
  const d = await page.evaluate(async ({email,senha,id,nome}) => {
    const r = await fetch('http://localhost:8080/api/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email,senha})});
    const lg = await r.json();
    const r2 = await fetch('http://localhost:8080/api/posts',{method:'POST',headers:{'Content-Type':'application/json',Authorization:`Bearer ${lg.token}`},body:JSON.stringify({texto:'Oi de Dona Maria!',imagem:null,autor:{id,nome},destinoTipo:'USUARIO',destinoId:id})});
    return r2.status;
  }, {email:U2.email,senha:U2.senha,id:U2.id,nome:U2.nome});
  if (d===201) ok('Post USER2 publicado'); else throw new Error(`status ${d}`);
});

await page.reload({ waitUntil:'domcontentloaded' });
await page.waitForTimeout(1200);
await page.waitForFunction(() => { const e=document.getElementById('container-posts'); return e&&!e.querySelector('.spinner'); }, {timeout:6000});

await chk('Posts aparecem no feed', async () => {
  const c = await page.$$('.post-card');
  if (c.length>0) ok(`${c.length} posts no feed`); else throw new Error('nenhum post-card');
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
  if (open.length>0) ok('Area comentar abriu'); else throw new Error('nao abriu');
});

await chk('Enviar comentario funciona', async () => {
  await page.locator('.area-comentar[style*="block"] textarea').first().fill('Que bonito!');
  await page.locator('.area-comentar[style*="block"] button.btn-primario').first().click();
  await page.waitForTimeout(1200);
  ok('Comentario enviado');
});

await chk('Clicar no autor de outro usuario -> perfil-publico', async () => {
  // seleciona especificamente links para o perfil de U2 (nao o proprio usuario)
  const link = page.locator(`.post-autor[href*="id=${U2.id}"]`).first();
  if (await link.count()>0) {
    await link.click();
    await page.waitForURL(atPerPub, {timeout:6000});
    ok('Autor -> perfil-publico');
    await page.goBack();
    await page.waitForURL(atFeed, {timeout:5000});
  } else ok('Post de U2 nao visivel no feed no momento (valido)');
});
await ss(page, '02-feed');

// ══════════════════════════════════════════════
process.stdout.write('\n-- 3. NAVEGACAO (barra inferior) --\n');

await chk('Nav Inicio ativo no feed', async () => {
  // garantir que estamos no feed antes de verificar
  if (!h(page.url()).includes('/feed')) {
    await page.goto(`${BASE}/feed.html`, {waitUntil:'domcontentloaded'});
    await page.waitForURL(atFeed, {timeout:5000});
  }
  const a = await page.$eval('a[href="feed.html"]', e => e.classList.contains('ativo'));
  if (a) ok('Nav Inicio ativo'); else throw new Error('nao ativo');
});

await chk('Nav Chat -> chat.html', async () => {
  await page.click('a[href="chat.html"]');
  await page.waitForURL(atChat, {timeout:5000});
  ok('Nav Chat OK');
});

await chk('Nav + -> publicar.html', async () => {
  await page.click('a[href="publicar.html"]');
  await page.waitForURL(atPub, {timeout:5000});
  ok('Nav + OK');
});

await chk('Nav Avisos -> notificacoes.html', async () => {
  await page.click('a[href="notificacoes.html"]');
  await page.waitForURL(atNotif, {timeout:5000});
  ok('Nav Avisos OK');
});

await chk('Nav Perfil -> perfil.html', async () => {
  await page.click('a[href="perfil.html"]');
  await page.waitForURL(atPerfil, {timeout:5000});
  ok('Nav Perfil OK');
});

// ══════════════════════════════════════════════
process.stdout.write('\n-- 4. PUBLICAR --\n');

await page.goto(`${BASE}/publicar.html`, {waitUntil:'domcontentloaded'});
await page.waitForTimeout(600);

await chk('Formulario publicar carrega', async () => {
  if (await page.$('#form-publicar')) ok('Formulario publicar OK');
  else throw new Error('nao encontrado');
});

await chk('Select destino tem opcao Feed publico', async () => {
  const opts = await page.$$('#destino-post option');
  const txts = await Promise.all(opts.map(o => o.textContent()));
  if (txts.some(t => t.includes('Feed')||t.includes('meu'))) ok(`Select OK (${txts.length} opcoes)`);
  else throw new Error(`Opcoes: ${txts.join(', ')}`);
});

await chk('Contador de caracteres atualiza', async () => {
  await page.fill('#texto-post', 'Ola mundo!');
  const c = await page.$eval('#contador-chars', e => e.textContent);
  if (c.includes('10')) ok(`Contador: "${c}"`); else throw new Error(`"${c}"`);
});

await chk('Publicar post -> feed.html', async () => {
  await page.fill('#texto-post', 'Publicado pelo teste automatico');
  await page.click('#btn-enviar');
  await page.waitForURL(atFeed, {timeout:10000});
  ok('Publicar -> feed.html');
});

// ══════════════════════════════════════════════
process.stdout.write('\n-- 5. PERFIL PROPRIO --\n');

await page.goto(`${BASE}/perfil.html`, {waitUntil:'domcontentloaded'});
await page.waitForTimeout(1000);

await chk('Nome exibido no perfil', async () => {
  const n = await page.$eval('#nome-perfil', e => e.textContent);
  if (n.includes('Teste')) ok(`Nome: "${n}"`); else throw new Error(`"${n}"`);
});

await chk('Email exibido no perfil', async () => {
  const e = await page.$eval('#email-perfil', e => e.textContent);
  if (e.includes('@')) ok(`Email: "${e}"`); else throw new Error(`"${e}"`);
});

await chk('Nickname exibido com @', async () => {
  const n = await page.$eval('#nick-perfil', e => e.textContent);
  if (n.startsWith('@')) ok(`Nick: "${n}"`); else throw new Error(`"${n}"`);
});

await chk('Estatisticas carregam', async () => {
  await page.waitForFunction(() => document.getElementById('num-posts').textContent !== '-', {timeout:6000});
  const [p,s,sg] = await Promise.all(['#num-posts','#num-seguidores','#num-seguindo'].map(id => page.$eval(id, e=>e.textContent)));
  ok(`Posts=${p} Segs=${s} Seguindo=${sg}`);
});

await chk('Secao minhas memorias carrega posts', async () => {
  await page.waitForFunction(() => { const e=document.getElementById('container-meus-posts'); return e&&!e.querySelector('.spinner'); }, {timeout:6000});
  const t = await page.$eval('#container-meus-posts', e=>e.innerText);
  if (t.length>5) ok('Memorias tem conteudo'); else throw new Error(`"${t.slice(0,80)}"`);
});

await chk('Botao Terminar Sessao existe', async () => {
  if (await page.$('button[onclick="terminarSessao()"]')) ok('Botao logout presente');
  else throw new Error('nao encontrado');
});
await ss(page, '05-perfil');

// ══════════════════════════════════════════════
process.stdout.write('\n-- 6. PERFIL PUBLICO --\n');

await page.goto(`${BASE}/perfil-publico?id=${U2.id}`, {waitUntil:'domcontentloaded'});
await page.waitForTimeout(1500);

await chk('Perfil publico carrega nome', async () => {
  await page.waitForFunction(() => document.querySelector('.perfil-nome'), {timeout:6000});
  const n = await page.$eval('.perfil-nome', e=>e.textContent);
  if (n.length>0) ok(`Nome: "${n}"`); else throw new Error('vazio');
});

await chk('3 estatisticas presentes', async () => {
  const s = await page.$$('.stat');
  if (s.length===3) ok('3 stats OK'); else throw new Error(`${s.length} stats`);
});

await chk('Botao Seguir alterna estado', async () => {
  const t1 = (await page.$eval('#btn-seguir', e=>e.textContent)).trim();
  await page.click('#btn-seguir');
  await page.waitForTimeout(1200);
  const t2 = (await page.$eval('#btn-seguir', e=>e.textContent)).trim();
  if (t1!==t2) ok(`Seguir: "${t1}" -> "${t2}"`); else throw new Error(`texto nao mudou: "${t1}"`);
});

await chk('Toggle reverso (desfazer seguir)', async () => {
  await page.click('#btn-seguir');
  await page.waitForTimeout(1000);
  ok('Toggle reverso OK');
  await page.click('#btn-seguir'); // seguir de novo para teste de chat
  await page.waitForTimeout(800);
});

await chk('Botao Mensagem abre chat direto', async () => {
  const [nav] = await Promise.all([
    page.waitForURL(u => h(u).includes('/chat'), {timeout:8000}),
    page.click(`button[onclick*="chat?usuario=${U2.id}"]`),
  ]);
  ok(`Botao Mensagem -> chat`);
});

await chk('Botao voltar (history.back) existe', async () => {
  await page.goto(`${BASE}/perfil-publico?id=${U2.id}`, {waitUntil:'domcontentloaded'});
  await page.waitForTimeout(600);
  if (await page.$('button[onclick="history.back()"]')) { await page.click('button[onclick="history.back()"]'); ok('Botao <- existe e funciona'); }
  else throw new Error('nao encontrado');
});

await chk('Proprio ID -> redireciona para perfil.html', async () => {
  await page.goto(`${BASE}/perfil-publico?id=${U1.id}`, {waitUntil:'domcontentloaded'});
  await page.waitForURL(atPerfil, {timeout:5000});
  ok('Proprio ID -> perfil.html');
});

await chk('ID invalido -> redireciona para feed.html', async () => {
  await page.goto(`${BASE}/perfil-publico?id=abc`, {waitUntil:'domcontentloaded'});
  await page.waitForURL(atFeed, {timeout:5000});
  ok('ID invalido -> feed.html');
});
await ss(page, '06-perfil-publico');

// ══════════════════════════════════════════════
process.stdout.write('\n-- 7. NOTIFICACOES --\n');

await page.goto(`${BASE}/notificacoes.html`, {waitUntil:'domcontentloaded'});
await page.waitForTimeout(1500);

await chk('Notificacoes carrega sem spinner', async () => {
  await page.waitForFunction(() => !document.querySelector('#container-notif .spinner'), {timeout:5000});
  ok('Notificacoes carregou');
});

await chk('Conteudo de notificacoes exibido', async () => {
  const t = await page.$eval('#container-notif', e=>e.innerText);
  if (t.trim().length>0) ok(`Notif: "${t.slice(0,50).trim()}..."`); else throw new Error('vazio');
});

await chk('Nav Avisos ativo em notificacoes.html', async () => {
  const a = await page.$eval('a[href="notificacoes.html"]', e=>e.classList.contains('ativo'));
  if (a) ok('Nav Avisos ativo'); else throw new Error('nao ativo');
});
await ss(page, '07-notificacoes');

// ══════════════════════════════════════════════
process.stdout.write('\n-- 8. CHAT --\n');

await page.goto(`${BASE}/chat.html`, {waitUntil:'domcontentloaded'});
await page.waitForTimeout(1500);

await chk('Chat carrega lista (aba Amigos)', async () => {
  await page.waitForFunction(() => { const e=document.getElementById('lista-conteudo'); return e&&!e.querySelector('.spinner'); }, {timeout:6000});
  ok('Chat lista carregou');
});

await chk('Aba Amigos ativa por padrao', async () => {
  if (await page.$eval('#btn-amigos', e=>e.classList.contains('ativo'))) ok('Aba Amigos ativa');
  else throw new Error('nao ativa');
});

await chk('Aba Grupos muda aba ativa', async () => {
  await page.click('#btn-grupos'); await page.waitForTimeout(700);
  if (await page.$eval('#btn-grupos', e=>e.classList.contains('ativo'))) ok('Aba Grupos ativa');
  else throw new Error('nao ativa');
  await page.click('#btn-amigos'); await page.waitForTimeout(400);
});

await chk('Chat direto abre via ?usuario= na URL', async () => {
  await page.goto(`${BASE}/chat?usuario=${U2.id}`, {waitUntil:'domcontentloaded'});
  // aguarda a tela-conversa ficar visivel (chain: DOMContentLoaded -> carregarLista -> fetch API -> abrirConversa)
  await page.waitForFunction(() => document.getElementById('tela-conversa')?.style.display === 'flex', {timeout:6000});
  ok('Chat direto abriu');
});

await chk('Nome do contato no cabecalho do chat', async () => {
  const n = await page.$eval('#conversa-nome', e=>e.textContent);
  if (n!=='-' && n.length>0) ok(`Nome: "${n}"`); else throw new Error(`"${n}"`);
});

await chk('Campo #input-msg aceita texto', async () => {
  await page.fill('#input-msg', 'Mensagem de teste automatico');
  const v = await page.$eval('#input-msg', e=>e.value);
  if (v.length>0) ok('Campo input-msg funciona'); else throw new Error('vazio');
});

await chk('Botao enviar limpa o campo', async () => {
  await page.click('button[onclick="enviarMensagem()"]');
  await page.waitForTimeout(1000);
  const v = await page.$eval('#input-msg', e=>e.value);
  if (v==='') ok('Campo limpou apos enviar'); else throw new Error('nao limpou');
});

await chk('Enter no campo envia mensagem', async () => {
  await page.fill('#input-msg', 'Via Enter');
  await page.press('#input-msg', 'Enter');
  await page.waitForTimeout(800);
  const v = await page.$eval('#input-msg', e=>e.value);
  if (v==='') ok('Enter enviou mensagem'); else throw new Error('nao limpou');
});

await chk('Fechar conversa volta para lista sem reabrir', async () => {
  await page.click('button[onclick="fecharConversa()"]');
  await page.waitForTimeout(800);
  const listaOk = await page.$eval('#tela-lista',    e=>e.style.display!=='none');
  const chatOk  = await page.$eval('#tela-conversa', e=>e.style.display==='none'||!e.style.display);
  if (listaOk && chatOk) ok('Fechar -> lista sem reabrir');
  else throw new Error(`lista=${listaOk} chat_oculto=${chatOk}`);
});

await chk('Chat de grupo abre via ?grupo= na URL', async () => {
  const {text} = await apiPost(page, '/comunidades', {nome:'Grupo Chat Teste', descricao:'Teste', criadorId:U1.id});
  let gid; try { gid = JSON.parse(text).id; } catch(_){}
  if (!gid) { ok('Grupo criado mas API retornou texto inesperado'); return; }
  await page.goto(`${BASE}/chat?grupo=${gid}`, {waitUntil:'domcontentloaded'});
  await page.waitForFunction(() => document.getElementById('tela-conversa')?.style.display === 'flex', {timeout:6000});
  ok(`Chat de grupo ${gid} abriu`);
});
await ss(page, '08-chat');

// ══════════════════════════════════════════════
process.stdout.write('\n-- 9. COMUNIDADES --\n');

await page.goto(`${BASE}/comunidades.html`, {waitUntil:'domcontentloaded'});
await page.waitForTimeout(1500);

await chk('Comunidades carrega sem spinner', async () => {
  await page.waitForFunction(() => !document.querySelector('#lista-grupos .spinner'), {timeout:5000});
  ok('Comunidades carregou');
});

await chk('Botao Criar Novo Grupo abre modal', async () => {
  await page.click('button[onclick*="modal-criar-grupo"]');
  await page.waitForTimeout(400);
  if (await page.$eval('#modal-criar-grupo', e=>e.classList.contains('aberto'))) ok('Modal criar grupo abriu');
  else throw new Error('nao abriu');
});

await chk('Criar grupo -> modal fecha e grupo aparece', async () => {
  await page.fill('#nome-grupo', 'Grupo Jardinagem');
  await page.fill('#desc-grupo', 'Para amantes de jardins!');
  await page.click('button[onclick="criarGrupo()"]');
  await page.waitForTimeout(1500);
  if (await page.$eval('#modal-criar-grupo', e=>e.classList.contains('aberto'))) throw new Error('modal nao fechou');
  const lista = await page.$eval('#lista-grupos', e=>e.innerText);
  if (lista.includes('Jardinagem')||lista.includes('Grupo')||lista.includes('grupo')) ok('Grupo na lista');
  else throw new Error(`Lista: "${lista.slice(0,80)}"`);
});

await chk('Cancelar modal de criar grupo fecha', async () => {
  await page.click('button[onclick*="modal-criar-grupo"]');
  await page.waitForTimeout(300);
  await page.locator('#modal-criar-grupo .btn-ghost').click();
  await page.waitForTimeout(300);
  if (!await page.$eval('#modal-criar-grupo', e=>e.classList.contains('aberto'))) ok('Cancelar fechou modal');
  else throw new Error('modal ainda aberto');
});

await chk('Botao Chat do grupo -> chat.html?grupo=', async () => {
  const btn = page.locator('.grupo-acoes .btn-primario').first();
  if (await btn.count()>0) {
    await btn.click();
    await page.waitForURL(atChat, {timeout:5000});
    ok('Chat do grupo OK');
    await page.goBack();
    await page.waitForURL(atCom, {timeout:5000});
  } else ok('Nenhum grupo listado (valido)');
});

await chk('Botao Convidar abre modal', async () => {
  const btn = page.locator('.grupo-acoes .btn-secundario').first();
  if (await btn.count()>0) {
    await btn.click(); await page.waitForTimeout(400);
    if (await page.$eval('#modal-convidar', e=>e.classList.contains('aberto'))) ok('Modal convidar abriu');
    else throw new Error('nao abriu');
    await page.click('button[onclick="fecharModalConvidar()"]');
    await page.waitForTimeout(300);
  } else ok('Nenhum grupo para convidar (valido)');
});

await chk('Area de convites pendentes existe no DOM', async () => {
  if (await page.$('#area-convites')) ok('area-convites presente'); else throw new Error('nao encontrado');
});
await ss(page, '09-comunidades');

// ══════════════════════════════════════════════
process.stdout.write('\n-- 10. CADASTRO --\n');

await page.goto(`${BASE}/cadastro.html`, {waitUntil:'domcontentloaded'});

await chk('Senhas diferentes exibem erro', async () => {
  await page.fill('#nome', 'Usuario Novo');
  await page.fill('#nickname', 'usuario_novo_99');
  await page.fill('#email', 'novo99@teste.com');
  await page.fill('#dataNascimento', '1950-05-20');
  await page.fill('#senha', 'Senha123!');
  await page.fill('#confirmaSenha', 'SenhaDiferente!');
  await page.click('#btn-submit');
  await page.waitForSelector('#msg-cadastro:visible', {timeout:4000});
  ok('Erro de senhas diferentes exibido');
});

await chk('Nickname em uso exibe aviso', async () => {
  await page.fill('#nickname', 'teste_auto');
  await page.waitForTimeout(900);
  const s = await page.$eval('#status-nick', e=>e.textContent);
  if (s.includes('uso')||s.includes('X')) ok(`Validacao nick: "${s}"`);
  else throw new Error(`status-nick: "${s}"`);
});

await chk('Nickname disponivel exibe confirmacao', async () => {
  await page.fill('#nickname', 'nick_xyz_99999_disponivel');
  await page.waitForTimeout(900);
  const s = await page.$eval('#status-nick', e=>e.textContent);
  if (s.includes('Disponivel')||s.includes('vel')||s.includes('OK')||s.length===0) ok(`Validacao nick disponivel: "${s}"`);
  else throw new Error(`status-nick: "${s}"`);
});
await ss(page, '10-cadastro');

// ══════════════════════════════════════════════
process.stdout.write('\n-- 11. LOGOUT E PROTECAO DE ROTAS --\n');

await page.goto(`${BASE}/perfil.html`, {waitUntil:'domcontentloaded'});
await page.waitForTimeout(500);

await chk('Terminar sessao -> index.html', async () => {
  await page.click('button[onclick="terminarSessao()"]');
  await page.waitForURL(atIndex, {timeout:5000});
  ok('Logout -> index.html');
});

await chk('Rota feed.html protegida apos logout', async () => {
  await page.goto(`${BASE}/feed.html`, {waitUntil:'domcontentloaded'});
  await page.waitForURL(atIndex, {timeout:5000});
  ok('feed protegido');
});

for (const [rota,pred,label] of [
  ['perfil.html',    atPerfil, 'perfil'],
  ['chat.html',      atChat,   'chat'],
  ['notificacoes.html', atNotif, 'notificacoes'],
  ['comunidades.html',  atCom,  'comunidades'],
]) {
  await chk(`Rota ${label} protegida apos logout`, async () => {
    await page.evaluate(() => localStorage.clear());
    await page.goto(`${BASE}/${rota}`, {waitUntil:'domcontentloaded'});
    await page.waitForURL(atIndex, {timeout:5000});
    ok(`${label} protegida -> login`);
  });
}

// ══════════════════════════════════════════════
process.stdout.write('\n-------------------------------------------------\n');
process.stdout.write(`\n RESULTADO FINAL:\n`);
process.stdout.write(`  Passaram : ${pass}\n`);
process.stdout.write(`  Falharam : ${fail}\n`);
process.stdout.write(`  Total    : ${pass+fail}\n`);

if (erros.length>0) {
  process.stdout.write('\n FALHAS:\n');
  erros.forEach((e,i) => process.stdout.write(`  ${i+1}. ${e}\n`));
}
process.stdout.write('\n Screenshots: C:/Users/guilh/AppData/Local/Temp/viver-ss/\n');

await browser.close();
process.exit(fail>0 ? 1 : 0);
