# Cardápio Digital

SaaS de cardápio digital e atendimento por mesa para bares e restaurantes.

## Requisitos

- Java 25, Node 22, Docker

## Rodando localmente

```bash
cp .env.example .env                 # preencha as senhas locais
docker compose up -d                 # Postgres local
cd backend && ./mvnw spring-boot:run # API em http://localhost:8080 (Swagger em /swagger-ui.html)
cd frontend && cp .env.example .env.local && npm install && npm run dev  # http://localhost:3000
```

## Fotos (Cloudflare R2)

As fotos dos produtos ficam num bucket do Cloudflare R2. Em desenvolvimento, use um bucket só seu, `cardapio-dev`, no plano gratuito. Os testes e o CI usam o S3Mock e não precisam disto.

1. No painel da Cloudflare, em **R2 Object Storage**, crie o bucket `cardapio-dev`.
2. Em **Settings** do bucket:
   - ligue a **Public Development URL** (r2.dev) e copie o endereço para `STORAGE_PUBLIC_URL`;
   - em **CORS Policy**, cole a política abaixo, que deixa o navegador enviar as fotos direto para o bucket.
3. Em **R2 Object Storage → Manage API tokens**, crie um token com **Object Read & Write** só para o `cardapio-dev`. Copie o *Access Key ID* e o *Secret Access Key* para o `.env`. Eles aparecem uma vez só.
4. O `STORAGE_ENDPOINT` é a **S3 API** mostrada nos detalhes do bucket, sem o nome do bucket no fim: `https://<account-id>.r2.cloudflarestorage.com`.

```json
[
  {
    "AllowedOrigins": ["http://localhost:3000"],
    "AllowedMethods": ["PUT"],
    "AllowedHeaders": ["Content-Type"],
    "MaxAgeSeconds": 3600
  }
]
```

Para testar pelo celular, acrescente o endereço da rede local (ex.: `http://192.168.0.10:3000`) em `AllowedOrigins`.

## Testar no celular (rede local)

Até o MVP, não há ambiente publicado: tudo roda na sua máquina (ADR 0005). Para abrir as telas no celular, conectado ao mesmo Wi-Fi:

```bash
hostname -I                                     # descubra o IP da máquina, ex.: 192.168.0.10
# no .env da raiz:      CORS_ALLOWED_ORIGINS=http://localhost:3000,http://192.168.0.10:3000
#                       REFRESH_COOKIE_SECURE=false  (o cookie da sessão só vale em https; sem isso, o F5 no painel pede login de novo)
#                       APP_PUBLIC_URL=http://192.168.0.10:3000  (o QR Code impresso aponta para cá)
# no frontend/.env.local: NEXT_PUBLIC_API_URL=http://192.168.0.10:8080
#                         DEV_ALLOWED_ORIGINS=192.168.0.10
cd frontend && npm run dev -- -H 0.0.0.0        # abra http://192.168.0.10:3000 no celular
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

## Publicação

Ainda não há staging nem produção. A publicação (Fly.io, Vercel e Neon, em São Paulo) acontece na Etapa 15 do roadmap, com o MVP pronto. Os arquivos já estão no repositório (`backend/Dockerfile`, `backend/fly.staging.toml`, `.github/workflows/deploy-staging.yml`), com o deploy desligado.
