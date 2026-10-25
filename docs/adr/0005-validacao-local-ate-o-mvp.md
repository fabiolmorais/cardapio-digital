# 0005 — Validação local até o MVP; publicação só no fim

**Status:** aceita (25/10/2026) · altera em parte a 0002 (o staging deixa de existir desde o início)

## Contexto

O plano era publicar um ambiente de testes (staging) já na Etapa 1: back no Fly.io, front na Vercel e banco no Neon. O Fly.io não tem plano gratuito para contas novas (só um teste de 2 horas de máquina ou 7 dias), e manter o staging antes de existir um restaurante usando o sistema gera custo e manutenção sem retorno.

## Decisão

- Até o MVP ficar pronto (Marco 4), tudo é desenvolvido e validado **localmente**: Docker Compose para o PostgreSQL, back e front rodando na máquina, e celulares de verdade acessando pela rede local.
- O **CI do GitHub** é a garantia de qualidade nesse período: build, testes com Testcontainers, ArchUnit, Playwright, Gitleaks e verificação de dependências a cada pull request.
- A **publicação** (contas no Fly.io, Vercel e Neon; staging e produção, todos em São Paulo) vira um bloco da Etapa 15, logo antes do Marco 4. O MVP é testado no staging antes da busca pelo restaurante piloto.
- Os arquivos de publicação já prontos (Dockerfile, `fly.staging.toml`, workflow `deploy-staging.yml`) ficam no repositório. O deploy automático só liga quando a variável `FLY_STAGING_ENABLED` for `true` no GitHub.

## Consequências

- Custo de hospedagem zero até o fim do MVP.
- A "definição de pronto" passa a exigir que a tarefa funcione no ambiente local e passe no CI, em vez de funcionar no staging.
- Medições que dependem de rede real (meta de 2 s do tempo real, abertura em 4G) são feitas localmente durante o desenvolvimento e confirmadas no staging na Etapa 15.
- Riscos de infraestrutura (memória do Spring no Fly.io, latência entre Fly.io e Neon, CORS e cookies entre domínios) só aparecem na Etapa 15; por isso ela reserva tempo para a publicação.
