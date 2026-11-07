// O teste de carga (k6): as mesas abrindo a página, lendo o cardápio e fazendo pedidos.
//
// Fase A (2 min): um restaurante com 30 mesas, todas ao mesmo tempo.
// Fase B (2 min, depois de uma pausa): vinte restaurantes com 10 mesas cada (200 celulares).
// Cada celular pede a cada ~22 s (o limite é 3 por minuto por celular) e lê a mesa, o cardápio e
// os pedidos a cada volta, como a tela faz.
//
// Uso: ver load/README.md (roda pelo Docker, com a imagem oficial do k6).
import exec from "k6/execution";
import http from "k6/http";
import { check, sleep } from "k6";

const API = __ENV.API_URL || "http://localhost:8080";
const { a, b } = JSON.parse(open("./restaurantes.json"));

export const options = {
  scenarios: {
    um_restaurante: { executor: "constant-vus", vus: 30, duration: "2m", exec: "mesa", env: { FASE: "A" } },
    vinte_restaurantes: {
      executor: "constant-vus",
      vus: 200,
      duration: "2m",
      startTime: "2m15s",
      exec: "mesa",
      env: { FASE: "B" },
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    "http_req_duration{acao:pedido}": ["p(95)<500"],
    "http_req_duration{acao:leitura}": ["p(95)<300"],
    checks: ["rate>0.99"],
  },
};

let joined = false;
let table;
let restaurant;
// O cookie da sessão vai no cabeçalho: ele sai com Secure, e o pote de cookies do k6 não o
// devolve por HTTP (no teste local). No staging, por HTTPS, daria no mesmo.
let cookie;

function pick() {
  const id = exec.vu.idInTest - 1;
  if (__ENV.FASE === "A") {
    restaurant = a;
    table = a.tables[id % a.tables.length];
  } else {
    const i = id % (b.length * 10);
    restaurant = b[Math.floor(i / 10)];
    table = restaurant.tables[i % 10];
  }
}

export function mesa() {
  if (!joined) {
    pick();
    const started = http.post(`${API}/api/v1/public/tables/${table}/session`);
    check(started, { "sessão aberta": (r) => r.status === 200 || r.status === 201 });
    cookie = `table_session=${started.cookies.table_session[0].value}`;
    check(http.post(`${API}/api/v1/public/tables/${table}/presence`, null, { headers: { Cookie: cookie } }), {
      "presença confirmada": (r) => r.status === 200,
    });
    joined = true;
  }
  const page = `${API}/api/v1/public/tables/${table}`;
  const read = { tags: { acao: "leitura" } };
  check(http.get(page, read), { "mesa abre": (r) => r.status === 200 });
  check(http.get(`${page}/menu`, read), { "cardápio abre": (r) => r.status === 200 });
  const order = http.post(
    `${page}/orders`,
    JSON.stringify({ items: [{ productId: restaurant.product, quantity: 1 + Math.floor(Math.random() * 3) }] }),
    { headers: { "Content-Type": "application/json", Cookie: cookie }, tags: { acao: "pedido" } },
  );
  check(order, { "pedido aceito": (r) => r.status === 201 || r.status === 200 });
  check(http.get(`${page}/orders`, { headers: { Cookie: cookie }, tags: { acao: "leitura" } }), {
    "pedidos abrem": (r) => r.status === 200,
  });
  sleep(21 + Math.random() * 3);
}
