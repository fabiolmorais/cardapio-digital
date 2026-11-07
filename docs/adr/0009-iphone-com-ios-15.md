# 0009 — Funcionar no iPhone com iOS 15 (Safari 15)

**Status:** aceita (07/11/2026)

## Contexto

No teste do staging, um iPhone 7 com iOS 15.8 abriu as telas, mas nenhum botão funcionava: o "Entrar" ficava parado e o olho da senha não respondia. O iPhone 7, o 6s e o SE de 1ª geração param no iOS 15 e ainda são comuns no Brasil. Na página da mesa, isso quer dizer um cliente que não consegue chamar o garçom nem pedir.

A causa: o Next 16 compila, por padrão, para Safari 16.4, Chrome 111 e Firefox 111. O código gerado tinha um bloco `static {}` dentro de uma classe (do próprio roteador do Next), que o Safari 15 não entende. O arquivo inteiro é rejeitado, e o React nunca liga a página.

No CSS, o Tailwind 4 usa `color-mix()` (Safari 16.2) para as cores com transparência (`bg-primary/15`). Para navegadores sem ele, o Tailwind escreve um plano B com a cor sólida: o fundo clarinho vira cor cheia, e o texto da mesma cor some.

## Decisão

- O front é compilado para **Safari 15, Chrome 87, Edge 88 e Firefox 78** (`browserslist` no `frontend/package.json`). O compilador reescreve o que esses navegadores não entendem.
- Depois do build (`postbuild`), `frontend/scripts/cores-safari-antigo.mjs`:
  - **falha o build** se o código gerado tiver bloco `static {}` (a trava para isso não voltar);
  - troca o plano B de cor sólida pela mesma cor do tema com a transparência certa (`rgba`). A cor de cada restaurante (`--brand`), desconhecida no build, fica transparente no plano B.

## Consequências

- A página da mesa ganhou cerca de 3 KB de JavaScript comprimido (188 → 191 KB), ainda dentro do orçamento de 200 KB.
- No Safari 15, as cores translúcidas usam as do tema escuro. No tema claro da página da mesa, o tom pode sair um pouco diferente, sem afetar a leitura.
- Os avisos no celular (web push) só existem no iPhone a partir do iOS 16.4: num iPhone 7, a equipe usa o painel aberto, com som.
- Os testes automáticos rodam no Chromium. A confirmação no Safari 15 é feita num iPhone de verdade, no staging (guia de testes).
