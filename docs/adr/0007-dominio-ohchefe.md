# 0007 — Domínio próprio: ohchefe.app.br

**Status:** aceita (06/11/2026)

## Contexto

A publicação (Etapa 15) precisa de um domínio: o cookie da sessão é `SameSite=Strict` e não passa entre `vercel.app` e `fly.dev`, então o front e a API precisam estar no mesmo site. A empresa (Hermez) terá outros produtos, e uma opção era usar subdomínios de `hermez.com.br`. Os QR Codes impressos apontam para o domínio do front, e trocar de domínio depois obrigaria a reimprimir os displays.

## Decisão

O cardápio tem domínio próprio, `ohchefe.app.br` (já registrado):

| Endereço | Uso |
| --- | --- |
| `ohchefe.app.br` | o front (Vercel); os QR Codes apontam para `ohchefe.app.br/m/{token}` |
| `api.ohchefe.app.br` | o back (Fly.io) |
| `staging.ohchefe.app.br` e `api.staging.ohchefe.app.br` | o staging |
| `fotos.ohchefe.app.br` | as fotos (domínio do bucket do R2 de produção) |
| `avisos.ohchefe.app.br` | o remetente dos e-mails (domínio verificado no Resend) |

`app.br` é um sufixo público (como `com.br`): `ohchefe.app.br` e `api.ohchefe.app.br` são o mesmo site para o navegador, e o cookie `SameSite=Strict` passa entre eles.

## Consequências

- Nenhum outro produto divide o domínio: o prefixo `__Host-` nos cookies, que protegeria contra um subdomínio vizinho, deixa de ser necessário (Guia 34).
- O DNS fica só com os registros acima; nada de curinga (`*.ohchefe.app.br`) nem registro apontando para serviço desligado.
- A reputação de envio de e-mail é só do cardápio; o SPF, o DKIM e o DMARC são configurados no `avisos.ohchefe.app.br`.
- O nome do produto na interface (antes "Cardápio Digital") passa a ser **OhChefe** (07/11/2026), com a landing page em `ohchefe.app.br` e o logo da mão levantada chamando o garçom ("Ô, chefe!").
