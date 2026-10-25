# 0002 — Spring Boot 4, Next.js e PostgreSQL

**Status:** aceita (21/10/2026)

## Contexto

Comparamos Spring Boot + Next.js + PostgreSQL com uma alternativa baseada em Supabase (custos e prazos em `especificacoes/custos-e-prazos.md`).

## Decisão

- Back-end: Java 21 + Spring Boot 4 (Web MVC, Security, Validation, Data JPA, Flyway), hospedado no Fly.io (São Paulo).
- Banco: PostgreSQL no Neon (São Paulo) em produção e staging; Docker localmente.
- Front-end: Next.js (App Router) + TypeScript + Tailwind + shadcn/ui + TanStack Query, na Vercel (São Paulo).
- Contrato: springdoc gera o OpenAPI; o front gera os tipos com openapi-typescript e consome com openapi-fetch.

## Consequências

- Stack que o desenvolvedor já domina; custo igual ou menor.
- O banco tem dois usuários: o do Flyway (dono das tabelas) e o da aplicação (só dados).
- Mudou a API, o front acusa erro de tipo depois de `npm run gen:api`; o CI falha se os tipos commitados estiverem desatualizados.
