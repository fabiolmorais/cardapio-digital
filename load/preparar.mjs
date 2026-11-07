// Prepara o teste de carga: cria os restaurantes de teste pela API e grava load/restaurantes.json.
// Um restaurante com 30 mesas (fase A) e vinte com 10 (fase B); cada um com uma mesa a mais, a da
// sonda, que mede o tempo real. O Wi-Fi do salão é a própria máquina (127.0.0.1 e ::1).
//
// Uso: node load/preparar.mjs   (com o back no ar; API_URL muda o endereço)
import { writeFileSync } from "node:fs";

const API = process.env.API_URL ?? "http://localhost:8080";
const PASSWORD = "carga2026teste";

async function call(method, path, body, token) {
  const response = await fetch(API + path, {
    method,
    headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!response.ok) throw new Error(`${method} ${path}: ${response.status} ${await response.text()}`);
  return response.status === 204 ? null : response.json();
}

async function restaurant(name, tables) {
  const email = `carga-${Date.now()}-${Math.round(Math.random() * 1e9)}@teste.com`;
  await call("POST", "/api/v1/signup", {
    establishmentName: name,
    ownerName: "Carga",
    document: "11222333000181",
    whatsapp: "81999999999",
    email,
    password: PASSWORD,
    acceptedTerms: true,
  });
  const { accessToken: token } = await call("POST", "/api/v1/auth/login", { email, password: PASSWORD });
  await call("PUT", "/api/v1/presence-rule", { networks: ["127.0.0.1", "::1"] }, token);
  const sector = await call("POST", "/api/v1/sectors", { name: "Salão" }, token);
  const tokens = [];
  for (let i = 1; i <= tables + 1; i++) {
    const table = await call("POST", "/api/v1/tables", { label: String(i).padStart(2, "0"), sectorId: sector.id }, token);
    tokens.push(table.publicToken);
  }
  const category = await call("POST", "/api/v1/menu-categories", { name: "Pastéis" }, token);
  const product = await call(
    "POST",
    "/api/v1/products",
    { categoryId: category.id, name: "Pastel de queijo", priceCents: 1290, featured: true, available: true },
    token,
  );
  return { name, email, password: PASSWORD, product: product.id, tables: tokens.slice(0, tables), probe: tokens[tables] };
}

const started = Date.now();
const a = await restaurant("Carga A", 30);
const b = await Promise.all(Array.from({ length: 20 }, (_, i) => restaurant(`Carga B${i + 1}`, 10)));
writeFileSync(new URL("./restaurantes.json", import.meta.url), JSON.stringify({ a, b }, null, 2));
console.log(`21 restaurantes prontos em ${((Date.now() - started) / 1000).toFixed(1)} s (load/restaurantes.json)`);
