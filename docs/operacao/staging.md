# Staging: publicação e segredos

O staging é a cópia do sistema na internet onde tudo é validado antes da produção (Etapa 15, ADR 0005 e 0007). Ele usa contas, banco, bucket e chaves **separados** da produção, e só dados fictícios.

| Parte | Onde | Endereço |
| --- | --- | --- |
| Front | Vercel, projeto `cardapio-digital`, ambiente `staging` | `https://staging.ohchefe.app.br` |
| Back | Fly.io, app `ohchefe-api-staging` (São Paulo) | `https://api.staging.ohchefe.app.br` |
| Banco | Neon, projeto `ohchefe`, branch `staging` | (segredo `DB_URL`) |
| Fotos | R2, bucket `ohchefe-fotos-staging` | `https://fotos-staging.ohchefe.app.br` |
| Vídeos | Bunny Stream, biblioteca `ohchefe-staging` | `vz-4bbe5cc8-e7e.b-cdn.net` |
| E-mail | Resend, domínio `avisos.ohchefe.app.br` | |
| Erros | Sentry, projetos `ohchefe-api` e `ohchefe-web` (ambiente `staging`) | |

## Como o código chega lá

- **Back:** o merge na `main` roda o CI; com ele verde, o workflow `Deploy staging` publica no Fly (`backend/fly.staging.toml`). Ele só roda com a variável do GitHub `FLY_STAGING_ENABLED=true` e usa o segredo `FLY_API_TOKEN` do ambiente `staging` (um token de deploy só deste app).
- **Front:** a Vercel publica a `main` no ambiente `staging` (Branch Tracking). O ambiente **Production** da Vercel acompanha a branch `producao`, que só vai existir na publicação da produção.
- O Flyway roda na subida do back, com o usuário dono das tabelas (`cardapio_owner`); a aplicação usa o `cardapio_app`, que não altera a estrutura.

## Configuração do back

O que **não** é segredo fica no `[env]` do `backend/fly.staging.toml`, versionado. Os segredos ficam só no cofre do Fly:

| Segredo | O que é |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://<host da branch staging, sem -pooler>/cardapio?sslmode=require` (o tempo real usa `LISTEN/NOTIFY`, que não passa pelo pooler) |
| `DB_PASSWORD` | a senha do `cardapio_app` na branch `staging` |
| `FLYWAY_PASSWORD` | a senha do `cardapio_owner` na branch `staging` |
| `JWT_SECRET` | gerado na hora (abaixo); trocar desloga todo mundo |
| `STORAGE_ACCESS_KEY_ID` e `STORAGE_SECRET_ACCESS_KEY` | o token do R2 restrito ao bucket do staging |
| `BUNNY_API_KEY` | a chave da **biblioteca** `ohchefe-staging` (não a da conta) |
| `VIDEO_WEBHOOK_TOKEN` | gerado na hora; vai também na URL do webhook do Bunny |
| `VAPID_PUBLIC_KEY` e `VAPID_PRIVATE_KEY` | gerados na hora; trocar invalida as inscrições de aviso no celular |
| `RESEND_API_KEY` | a chave `ohchefe-api` da Resend |

### Cadastrar os segredos

Os comandos abaixo nunca mostram os valores na tela nem os gravam no histórico do terminal. O `--stage` grava sem publicar (a publicação vem pelo merge).

1. Os que vêm do gerenciador de senhas: rode o comando, cole as linhas `NOME=valor` (uma por linha) e termine com **Ctrl+D**:

   ```bash
   fly secrets import --stage -a ohchefe-api-staging
   ```

   ```text
   DB_URL=jdbc:postgresql://<host>/cardapio?sslmode=require
   DB_PASSWORD=...
   FLYWAY_PASSWORD=...
   STORAGE_ACCESS_KEY_ID=...
   STORAGE_SECRET_ACCESS_KEY=...
   BUNNY_API_KEY=...
   RESEND_API_KEY=...
   ```

2. Os gerados na hora (o valor vai direto para o cofre):

   ```bash
   fly secrets set --stage -a ohchefe-api-staging JWT_SECRET="$(openssl rand -base64 48)"
   ```

   ```bash
   source ~/.nvm/nvm.sh   # Node 22: o par VAPID usa a exportação JWK
   node -e '
     const { generateKeyPairSync } = require("node:crypto");
     const jwk = generateKeyPairSync("ec", { namedCurve: "prime256v1" }).privateKey.export({ format: "jwk" });
     const pub = Buffer.concat([Buffer.from([4]), Buffer.from(jwk.x, "base64url"), Buffer.from(jwk.y, "base64url")]);
     console.log(`VAPID_PUBLIC_KEY=${pub.toString("base64url")}\nVAPID_PRIVATE_KEY=${jwk.d}`);
   ' | fly secrets import --stage -a ohchefe-api-staging
   ```

3. O token do webhook do vídeo, que também vai no painel do Bunny: gere, cadastre e copie para o gerenciador de senhas antes de fechar o terminal.

   ```bash
   T="$(openssl rand -hex 24)"; fly secrets set --stage -a ohchefe-api-staging VIDEO_WEBHOOK_TOKEN="$T"; echo "$T"; unset T
   ```

4. Confira só os nomes (o Fly nunca mostra os valores): `fly secrets list -a ohchefe-api-staging`.

## Configuração do front (Vercel)

No projeto `cardapio-digital`:

1. **Settings → Environments → Production → Branch Tracking:** troque a branch para `producao`.
2. **Settings → Environments → Create Environment:** `staging`, com **Branch Tracking** na `main` e o domínio `staging.ohchefe.app.br`.
3. **Settings → Environment Variables**, só no ambiente `staging`:

   | Variável | Valor |
   | --- | --- |
   | `API_URL` | `https://api.staging.ohchefe.app.br` |
   | `NEXT_PUBLIC_API_URL` | `https://api.staging.ohchefe.app.br` |
   | `SENTRY_DSN` e `NEXT_PUBLIC_SENTRY_DSN` | o DSN do projeto `ohchefe-web` |
   | `SENTRY_ENVIRONMENT` e `NEXT_PUBLIC_SENTRY_ENVIRONMENT` | `staging` |
   | `SENTRY_ORG` | `ohchefe` |
   | `SENTRY_PROJECT` | `ohchefe-web` |
   | `SENTRY_AUTH_TOKEN` | o Organization Token do Sentry, marcado como **Sensitive** |

   As `NEXT_PUBLIC_*` entram no JavaScript na hora do build: depois de mudar, publique de novo.

## DNS (Cloudflare, zona `ohchefe.app.br`)

Todos como **DNS only** (nuvem cinza): o Fly e a Vercel emitem o próprio certificado e precisam receber o tráfego direto.

| Tipo | Nome | Valor |
| --- | --- | --- |
| CNAME | `api.staging` | `ohchefe-api-staging.fly.dev` |
| CNAME | `staging` | o valor que a Vercel mostrar ao adicionar o domínio |

Depois do primeiro deploy do back: `fly certs add api.staging.ohchefe.app.br -a ohchefe-api-staging` e `fly certs check api.staging.ohchefe.app.br -a ohchefe-api-staging` até o certificado sair.

## Webhook do Bunny

Com o back no ar, na biblioteca `ohchefe-staging`, aba **API → Webhook URL**:

```text
https://api.staging.ohchefe.app.br/api/v1/webhooks/video?token=<VIDEO_WEBHOOK_TOKEN>
```

## Conferência depois de publicar

- `https://api.staging.ohchefe.app.br/actuator/health` responde `{"status":"UP"}`, e `/v3/api-docs` responde 404.
- Criar conta, entrar, cadastrar produto com foto e com vídeo (o vídeo fica pronto sozinho pelo webhook).
- Abrir a mesa pelo QR Code no celular (4G), chamar o garçom e fazer pedido: o painel recebe em menos de 2 s.
- "Esqueci a senha": o e-mail chega, vindo de `avisos.ohchefe.app.br`.
- Um erro de teste aparece no Sentry, no ambiente `staging`.
- Teste de carga (`load/README.md`) apontando para o staging.
