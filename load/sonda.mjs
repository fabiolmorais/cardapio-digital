// A sonda do tempo real: mede quanto um pedido leva da mesa até a tela da equipe (meta: 2 s).
//
// Para cada restaurante de load/restaurantes.json, ela:
// - abre o tempo real da equipe (como o painel aberto);
// - abre o tempo real de cada mesa (como os celulares abertos na mesa);
// - faz um pedido pela mesa da sonda a cada 22 s (o limite é 3 por minuto por celular) e mede o
//   tempo do clique até o aviso "order" daquele pedido chegar no tempo real da equipe.
//
// Uso: node load/sonda.mjs <segundos>   (roda junto com o k6; API_URL muda o endereço)
const API = process.env.API_URL ?? "http://localhost:8080";
const SECONDS = Number(process.argv[2] ?? 300);
const EVERY_MS = 22_000; // 3 por minuto por celular: com 20 s, o 4º cairia na mesma janela
const { a, b } = JSON.parse((await import("node:fs")).readFileSync(new URL("./restaurantes.json", import.meta.url), "utf8"));
const restaurants = [a, ...b];

const pending = new Map(); // orderId → instante do clique (esperando o aviso)
const arrived = new Map(); // orderId → instante do aviso (que chegou antes da resposta)
const samples = []; // { at, ms, phase }
const start = performance.now();
let lost = 0;

/** Lê um fluxo SSE e chama onEvent(nome, dados) a cada aviso. */
async function stream(path, headers, onEvent) {
  const response = await fetch(API + path, { headers: { Accept: "text/event-stream", ...headers } });
  if (!response.ok) throw new Error(`SSE ${path}: ${response.status}`);
  const reader = response.body.pipeThrough(new TextDecoderStream()).getReader();
  (async () => {
    let buffer = "";
    for (;;) {
      const { value, done } = await reader.read().catch(() => ({ done: true }));
      if (done) return;
      buffer += value;
      let cut;
      while ((cut = buffer.indexOf("\n\n")) >= 0) {
        const block = buffer.slice(0, cut);
        buffer = buffer.slice(cut + 2);
        const name = /^event:(.*)$/m.exec(block)?.[1]?.trim();
        const data = /^data:(.*)$/m.exec(block)?.[1];
        if (name && data) onEvent(name, JSON.parse(data));
      }
    }
  })();
}

async function session(qr) {
  const response = await fetch(`${API}/api/v1/public/tables/${qr}/session`, { method: "POST" });
  const cookie = response.headers.getSetCookie().find((c) => c.startsWith("table_session="));
  return cookie.split(";")[0];
}

async function login(r) {
  const response = await fetch(`${API}/api/v1/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: r.email, password: r.password }),
  });
  return (await response.json()).accessToken;
}

// Os celulares das mesas, com o tempo real aberto (a conexão fica parada, só ocupando o servidor).
let phones = 0;
for (const r of restaurants) {
  const token = await login(r);
  await stream("/api/v1/events", { Authorization: `Bearer ${token}` }, (name, data) => {
    if (name !== "order") return;
    const now = performance.now();
    if (!pending.has(data.orderId)) {
      arrived.set(data.orderId, now);
      return;
    }
    const clicked = pending.get(data.orderId);
    pending.delete(data.orderId);
    samples.push({ at: (now - start) / 1000, ms: now - clicked });
  });
  for (const qr of r.tables) {
    const cookie = await session(qr);
    await stream(`/api/v1/public/tables/${qr}/events`, { Cookie: cookie }, () => {});
    phones++;
  }
  r.cookie = await session(r.probe);
  await fetch(`${API}/api/v1/public/tables/${r.probe}/presence`, { method: "POST", headers: { Cookie: r.cookie } });
}
console.log(`sonda: ${restaurants.length} painéis e ${phones} celulares com o tempo real aberto`);

async function probe(r) {
  const clicked = performance.now();
  const response = await fetch(`${API}/api/v1/public/tables/${r.probe}/orders`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Cookie: r.cookie },
    body: JSON.stringify({ items: [{ productId: r.product, quantity: 1 }] }),
  });
  if (!response.ok) {
    console.log(`sonda: pedido recusado (${response.status}) em ${r.name}`);
    return;
  }
  const order = await response.json();
  // O aviso pode ter chegado antes da resposta: aí a conta já fecha aqui.
  if (arrived.has(order.id)) {
    const now = arrived.get(order.id);
    arrived.delete(order.id);
    samples.push({ at: (now - start) / 1000, ms: now - clicked });
    return;
  }
  pending.set(order.id, clicked);
  setTimeout(() => {
    if (pending.delete(order.id)) lost++;
  }, 10_000);
}

// Os pedidos da sonda, espalhados: cada restaurante no seu momento dentro dos 22 s.
const timers = restaurants.map((r, i) =>
  setTimeout(() => {
    void probe(r);
    timers.push(setInterval(() => void probe(r), EVERY_MS));
  }, (EVERY_MS / restaurants.length) * i),
);

function report(label, list) {
  if (list.length === 0) return `${label}: sem amostras`;
  const ms = list.map((s) => s.ms).sort((x, y) => x - y);
  const p = (q) => ms[Math.min(ms.length - 1, Math.floor(q * ms.length))].toFixed(0);
  const over = ms.filter((v) => v > 2000).length;
  return `${label}: ${ms.length} pedidos · p50 ${p(0.5)} ms · p95 ${p(0.95)} ms · máx ${ms.at(-1).toFixed(0)} ms · acima de 2 s: ${over}`;
}

setTimeout(() => {
  console.log("\nTempo do pedido até a tela da equipe (meta: 2 s)");
  console.log(report("tudo", samples));
  for (let minute = 0; minute * 60 < SECONDS; minute++) {
    console.log(report(`  minuto ${minute + 1}`, samples.filter((s) => s.at >= minute * 60 && s.at < (minute + 1) * 60)));
  }
  console.log(`avisos que não chegaram em 10 s: ${lost}`);
  process.exit(lost > 0 || samples.some((s) => s.ms > 2000) ? 1 : 0);
}, SECONDS * 1000);
