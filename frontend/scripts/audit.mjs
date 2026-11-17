// A conferência de dependências do CI: o `npm audit` das dependências de produção, falhando em
// qualquer falha alta ou crítica, menos as exceções de audit-excecoes.json. Cada exceção tem motivo
// e prazo de revisão; vencido o prazo, ou publicada a versão corrigida, o CI volta a falhar (para
// atualizarmos na hora). Sem dependência nova: só o npm e o Node.
import { execFileSync } from "node:child_process";
import { readFileSync } from "node:fs";

const LEVELS = new Set(["high", "critical"]);
const today = new Date().toISOString().slice(0, 10);
const exceptions = JSON.parse(readFileSync(new URL("../audit-excecoes.json", import.meta.url), "utf8"));
const allowed = new Map(exceptions.map((e) => [e.id, e]));

// O npm audit sai com código diferente de zero quando acha falhas: o relatório vem no stdout mesmo assim.
let raw;
try {
  raw = execFileSync("npm", ["audit", "--omit=dev", "--json"], { encoding: "utf8", maxBuffer: 64 << 20 });
} catch (error) {
  raw = error.stdout;
}
const report = JSON.parse(raw);

// Os alertas (GHSA) de severidade alta ou crítica, cada um uma vez.
const advisories = new Map();
for (const vulnerability of Object.values(report.vulnerabilities ?? {})) {
  for (const via of vulnerability.via) {
    if (typeof via !== "object" || !LEVELS.has(via.severity)) continue;
    const id = via.url?.split("/").pop();
    if (id) advisories.set(id, { id, name: via.name, title: via.title, severity: via.severity, url: via.url });
  }
}

const problems = [];
for (const advisory of advisories.values()) {
  const exception = allowed.get(advisory.id);
  if (!exception) {
    problems.push(`${advisory.severity}: ${advisory.name}: ${advisory.title} (${advisory.url})`);
  } else if (exception.revisarAte < today) {
    problems.push(`${advisory.id} (${advisory.name}): a exceção venceu em ${exception.revisarAte}. Revise.`);
  } else {
    console.log(`Exceção até ${exception.revisarAte}: ${advisory.id} (${advisory.name}). ${exception.motivo}`);
  }
}

// Saiu a versão corrigida de uma exceção? Então ela não serve mais: é hora de atualizar.
for (const exception of exceptions) {
  try {
    const response = await fetch(`https://api.github.com/advisories/${exception.id}`, {
      headers: { Accept: "application/vnd.github+json" },
    });
    if (!response.ok) throw new Error(String(response.status));
    const advisory = await response.json();
    const patched = (advisory.vulnerabilities ?? []).find((v) => v.first_patched_version);
    if (patched) {
      problems.push(
        `${exception.id}: saiu a correção (${patched.package.name} ${patched.first_patched_version}). ` +
          "Atualize a dependência e tire a exceção de audit-excecoes.json.",
      );
    }
  } catch (error) {
    console.warn(`Não deu para consultar ${exception.id} no GitHub (${error.message}): sigo sem essa conferência.`);
  }
}

if (problems.length > 0) {
  console.error(`Falhas de segurança nas dependências:\n- ${problems.join("\n- ")}`);
  process.exit(1);
}
console.log("Dependências sem falha alta ou crítica fora das exceções.");
