# Cardápio Digital

SaaS de cardápio digital e atendimento por mesa para bares e restaurantes.

## Requisitos

- Java 21, Node 22, Docker

## Rodando localmente

```bash
cp .env.example .env                 # preencha as senhas locais
docker compose up -d                 # Postgres local
cd backend && ./mvnw spring-boot:run # API em http://localhost:8080 (Swagger em /swagger-ui.html)
cd frontend && cp .env.example .env.local && npm install && npm run dev  # http://localhost:3000
```

## Comandos

```bash
cd backend && ./mvnw verify          # testes do back (unitários, integração com Testcontainers, ArchUnit)
cd frontend && npm run gen:api       # gera os tipos a partir do OpenAPI (com o back rodando)
cd frontend && npm run test:e2e      # Playwright (com o back rodando)
```

## Estrutura

- `backend/` — Spring Boot 4, arquitetura hexagonal
- `frontend/` — Next.js (App Router)
- `docs/adr/` — decisões de arquitetura
