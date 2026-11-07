# Teste de carga

Mede se o sistema aguenta um salão cheio e vários restaurantes ao mesmo tempo, e se o tempo real cumpre a meta de **2 segundos** da ação até a tela (CLAUDE.md, "Tempo real").

| Fase | O que simula | Duração |
| --- | --- | --- |
| A | um restaurante com 30 mesas pedindo ao mesmo tempo | 2 min |
| B | vinte restaurantes com 10 mesas cada (200 celulares) | 2 min |

Cada celular abre a mesa, lê o cardápio, faz um pedido e lê os pedidos a cada ~22 s (o limite é 3 pedidos por minuto por celular). Enquanto isso, a sonda mantém aberto o tempo real de todos os painéis e de todas as mesas, e mede quanto o pedido de uma mesa extra leva até chegar na tela da equipe.

## As peças

- `preparar.mjs` (Node): cria os 21 restaurantes de teste pela API e grava `restaurantes.json`.
- `sonda.mjs` (Node): o tempo real aberto e a medição dos 2 segundos.
- `carga.js` (k6): as mesas. Roda pela imagem oficial do k6 no Docker: nenhuma dependência no projeto.
- `rodar.sh`: roda tudo na ordem.

## Como rodar

1. Suba o banco e o back. **Desligue o limite por IP** (`RATE_LIMIT_ENABLED=false` no `.env`): toda a carga sai de um IP só.
2. Na raiz do repositório:

```bash
./load/rodar.sh
```

No fim saem o resumo do k6 (`resultado-k6.txt`) e o da sonda (`resultado-sonda.txt`). Os restaurantes de teste ficam no banco local (com e-mails `carga-…@teste.com`).

**Passou** quando:

- o k6 não marca nenhum limite (`thresholds`) como falho: menos de 1% de erro, pedido com p95 abaixo de 500 ms e leitura abaixo de 300 ms;
- a sonda não tem pedido acima de 2 s nem aviso perdido.

No staging (Etapa 15), rode com `API_URL=https://api.staging.ohchefe.app.br`, e de outra máquina: a medição local não tem a rede de verdade no meio.

## Resultados

### 06/11/2026 — local (back, banco, k6 e sonda no mesmo notebook; back no modo de desenvolvimento)

A primeira rodada **travou o back** (Java 21): um impasse das threads virtuais, explicado no [ADR 0008](../docs/adr/0008-java-25.md). Corrigido com a troca para o Java 25. A rodada abaixo já é no Java 25, com as threads virtuais ligadas.

| Medida | Resultado | Meta |
| --- | --- | --- |
| Requisições com erro | 0 de 5.936 | menos de 1% |
| Pedido do celular, p95 | 1,18 s | 500 ms |
| Leitura (mesa, cardápio, pedidos), p95 | 568 ms | 300 ms |
| Tempo real, do pedido até a tela da equipe | p95 100 ms, máximo 1,6 s, nenhum acima de 2 s | 2 s |
| Avisos do tempo real perdidos | 0 de 273 | 0 |
| Cinco quedas bruscas de 251 conexões | o back continuou respondendo | sem travar |

- **O tempo real cumpre a meta de 2 s com folga**, com 230 celulares e 21 painéis abertos.
- **Os tempos das requisições ficaram acima das metas**, puxados pelos picos em que 200 celulares começam no mesmo segundo (o minuto 2 e 3 da sonda). Com o back, o banco e a carga disputando o mesmo processador, a medida que vale é a do staging. Lá, conferir também o tamanho do pool de conexões do banco (o Hikari usa 10 por padrão).
