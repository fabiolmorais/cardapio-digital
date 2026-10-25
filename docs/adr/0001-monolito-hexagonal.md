# 0001 — Monolito com arquitetura hexagonal

**Status:** aceita (21/10/2026)

## Contexto

O sistema começa com um desenvolvedor e precisa evoluir rápido, mas tem regras de negócio ricas (chamados, pedidos, comanda, rodízio, trava de presença, planos) e integrações que podem mudar de fornecedor (R2, Bunny, Resend, Asaas).

## Decisão

Uma única aplicação Spring Boot, organizada em portas e adaptadores: `domain` (Java puro), `application` (casos de uso e portas), `adapter` (web, persistência, integrações) e `config` (montagem dos beans). As regras de dependência são verificadas por ArchUnit no `./mvnw verify`.

## Consequências

- Deploy e operação simples (um container no Fly.io).
- Regras de negócio testáveis sem Spring e sem banco.
- Trocar um fornecedor muda só o adaptador.
- Mais classes por funcionalidade (portas, mappers, DTOs) do que um CRUD direto.
