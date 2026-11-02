# 0006 — O cardápio público é um documento, não uma lista paginada

**Status:** aceita (02/11/2026) · abre uma exceção à regra "listas sempre paginadas" do CLAUDE.md

## Contexto

A página da mesa (`/m/{token}`) é montada no servidor do Next e precisa abrir em até 2 segundos no 4G. Ela mostra o cardápio inteiro: as abas de categoria, os destaques e a busca, que roda no próprio celular, sem nova ida à rede a cada letra.

A regra do projeto é "listas sempre paginadas", para nenhuma resposta crescer sem limite. Paginando o cardápio, o servidor do Next teria de buscar página por página e juntar tudo antes de montar a tela: mais chamadas, mais código e uma página mais lenta, para chegar ao mesmo resultado.

## Decisão

- `GET /api/v1/public/tables/{token}/menu` devolve o cardápio **inteiro**, como **um recurso só** (as categorias, cada uma com os seus produtos), e não como uma lista paginada.
- O tamanho fica limitado por um **teto de 500 produtos ativos por restaurante** (`Product.MAX_PER_ESTABLISHMENT`), conferido ao criar um produto (422 acima disso).
- A montagem usa um número fixo de consultas (categorias, produtos e capas), para não haver N+1 qualquer que seja o tamanho do cardápio.
- As listas do painel de gestão continuam paginadas.

## Consequências

- Uma chamada só para a página da mesa, e a busca e as abas funcionam sem rede.
- No teto, a resposta tem poucas centenas de KB de JSON (sem as fotos, que vêm por URL e só as visíveis carregam).
- Se um restaurante precisar de mais de 500 produtos, a decisão é revista; os limites por plano (Etapa 14) podem baixar o teto.
