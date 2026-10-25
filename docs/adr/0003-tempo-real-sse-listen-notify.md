# 0003 — Tempo real com SSE e LISTEN/NOTIFY

**Status:** aceita (21/10/2026)

## Contexto

O chamado da mesa precisa chegar à equipe em até 2 segundos. O fluxo é quase todo do servidor para a tela, e pode haver mais de uma instância do back-end.

## Decisão

Server-Sent Events do Spring para as telas (um canal por restaurante e um por sessão de mesa) e `LISTEN/NOTIFY` do PostgreSQL para repassar eventos entre instâncias. Payload mínimo: tipo, IDs e status.

## Consequências

- Sem Redis ou broker no início.
- Reconexão automática do navegador com o SSE.
- Se a escala exigir, o `LISTEN/NOTIFY` pode ser trocado por um broker sem mudar o domínio (é um adaptador de saída).
