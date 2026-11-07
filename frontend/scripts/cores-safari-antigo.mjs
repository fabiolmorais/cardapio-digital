// Roda depois do `next build` (npm "postbuild"), pelo iPhone com iOS 15 (iPhone 7, 6s, SE de 1ª
// geração), que para no Safari 15 (ADR 0009). Confere que o JavaScript roda nele e ajusta, no CSS
// final, o "plano B" que o Tailwind escreve para os navegadores sem color-mix() (antes do 16.2).
//
// Para uma cor do tema com transparência (bg-primary/15), o Tailwind gera:
//   .bg-primary\/15{background-color:var(--primary)}
//   @supports (color:color-mix(in lab, red, red)){.bg-primary\/15{background-color:color-mix(in oklab, var(--primary) 15%, transparent)}}
// O navegador antigo fica só com a primeira regra: a cor SÓLIDA. Onde o texto tem a mesma cor do
// fundo (os selos "Ao vivo", por exemplo), ele some. Aqui o plano B vira a mesma cor com a
// transparência certa (rgba), calculada das cores do tema escuro (globals.css, .dark). Os
// navegadores novos continuam usando o color-mix(), que vem depois e vence.
//
// As cores que cada restaurante escolhe (--brand) não são conhecidas no build: o plano B delas
// vira transparente. As outras variáveis sem valor fixo (--mesa-bg) ficam como o Tailwind deixou.
import { readFileSync, readdirSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const ROOT = new URL("..", import.meta.url).pathname;
const CSS_DIR = join(ROOT, ".next/static");
const DYNAMIC_TO_TRANSPARENT = new Set(["brand"]);

/** As cores do tema escuro: --nome: #rrggbb ou rgb(r g b / a%). */
function themeColors() {
  const css = readFileSync(join(ROOT, "src/app/globals.css"), "utf8");
  const dark = css.slice(css.indexOf(".dark {"), css.indexOf("}", css.indexOf(".dark {")));
  const colors = new Map();
  for (const [, name, value] of dark.matchAll(/--([a-z0-9-]+):\s*([^;]+);/g)) {
    const rgba = parseColor(value.trim());
    if (rgba) colors.set(name, rgba);
  }
  return colors;
}

function parseColor(value) {
  let m = /^#([0-9a-f]{3}|[0-9a-f]{6})$/i.exec(value);
  if (m) {
    const hex = m[1].length === 3 ? [...m[1]].map((c) => c + c).join("") : m[1];
    return [0, 2, 4].map((i) => parseInt(hex.slice(i, i + 2), 16)).concat(1);
  }
  m = /^rgb\(\s*(\d+)\s+(\d+)\s+(\d+)\s*(?:\/\s*([\d.]+)(%?))?\s*\)$/.exec(value);
  if (m) return [Number(m[1]), Number(m[2]), Number(m[3]), m[4] === undefined ? 1 : Number(m[4]) / (m[5] ? 100 : 1)];
  return null;
}

/** Separa a lista de seletores nas vírgulas de verdade (não nas escapadas, como em `\,`). */
function splitSelectors(list) {
  return list.split(/(?<!\\),/);
}

const PAIR =
  /([^{}@]+)\{([a-z-]+):var\(--([a-z0-9-]+)\)\}(@supports \(color:color-mix\(in lab, red, red\)\)\{([^{}]+)\{\2:color-mix\(in oklab, var\(--\3\) ([\d.]+)%, transparent\)\}\})/g;

function fix(css, colors) {
  let changed = 0;
  const out = css.replace(PAIR, (whole, list, prop, token, supports, selector, percent) => {
    let fallback;
    if (colors.has(token)) {
      const [r, g, b, a] = colors.get(token);
      fallback = `rgba(${r},${g},${b},${+(a * (Number(percent) / 100)).toFixed(4)})`;
    } else if (DYNAMIC_TO_TRANSPARENT.has(token)) {
      fallback = "transparent";
    } else {
      return whole;
    }
    changed++;
    const others = splitSelectors(list).filter((s) => s !== selector);
    const kept = others.length ? `${others.join(",")}{${prop}:var(--${token})}` : "";
    return `${kept}${selector}{${prop}:${fallback}}${supports}`;
  });
  return { out, changed };
}

function cssFiles(dir) {
  return readdirSync(dir, { withFileTypes: true }).flatMap((e) =>
    e.isDirectory() ? cssFiles(join(dir, e.name)) : e.name.endsWith(".css") ? [join(dir, e.name)] : [],
  );
}

/**
 * A trava do JavaScript: o bloco static {} em classes só existe a partir do Safari 16.4. Se ele
 * aparecer no código gerado (o browserslist do package.json saiu do Safari 15, ou uma biblioteca
 * nova o trouxe), o iPhone com iOS 15 não roda NADA da página. Melhor o build falhar aqui.
 */
function jsFiles(dir) {
  return readdirSync(dir, { withFileTypes: true }).flatMap((e) =>
    e.isDirectory() ? jsFiles(join(dir, e.name)) : e.name.endsWith(".js") ? [join(dir, e.name)] : [],
  );
}
const withStaticBlock = jsFiles(CSS_DIR).filter((f) => /[{};]static\{/.test(readFileSync(f, "utf8")));
if (withStaticBlock.length) {
  console.error(
    `O código gerado tem bloco "static {}" em classe, que o Safari 15 (iPhone com iOS 15) não entende:\n  ${withStaticBlock.join("\n  ")}\n` +
      'Confira o "browserslist" do package.json (precisa incluir "safari 15").',
  );
  process.exit(1);
}

const colors = themeColors();
let total = 0;
for (const file of cssFiles(CSS_DIR)) {
  const { out, changed } = fix(readFileSync(file, "utf8"), colors);
  if (changed) writeFileSync(file, out);
  total += changed;
}
console.log(`Cores para o Safari antigo: ${total} regra(s) ajustada(s).`);
