# Assinaturas no piloto

Até a cobrança automática pela Efí (fase 2), o pagamento da assinatura é combinado por fora (Pix ou transferência) e a ativação é feita por nós, com um script SQL. Nenhuma rota nova fica exposta na internet para isso.

## Como funciona para o restaurante

1. O cadastro começa com **14 dias de teste grátis**, com os limites do Pro.
2. Na tela **Assinatura** do admin, o dono escolhe o plano e o ciclo (mensal ou anual). A escolha fica registrada, à espera do pagamento; os limites continuam os do plano atual.
3. Os avisos por e-mail saem 3 dias antes, 1 dia antes e no fim do teste (até a Etapa 15, só registrados no log).
4. Depois do fim do que está coberto (o teste ou o período pago), há **7 dias de tolerância** com tudo funcionando e o aviso "assinatura pendente" no admin.
5. Passada a tolerância, a página da mesa para de chamar e pedir; o painel e a gestão continuam abrindo.

## Quem escolheu um plano e espera o pagamento

```sql
SELECT e.name AS restaurante, e.slug, p.name AS plano, s.requested_cycle AS ciclo, s.requested_at AS escolhido_em,
       o.email
FROM subscription s
JOIN organization o ON o.id = s.organization_id
JOIN establishment e ON e.organization_id = s.organization_id
JOIN plan p ON p.id = s.requested_plan_id
ORDER BY s.requested_at;
```

## Ativar ou renovar, depois do pagamento

```bash
psql "$DATABASE_URL" -v slug=sakura-sushi -f docs/operacao/ativar-assinatura.sql
```

O script ([ativar-assinatura.sql](ativar-assinatura.sql)) passa para o plano escolhido, marca como paga por 1 mês (ou 1 ano) a partir do fim do que já estava coberto e limpa a escolha. A resposta mostra o restaurante, o plano, o ciclo e até quando está pago. O teste `ActivationScriptTest` roda este mesmo arquivo a cada build.

Para um plano diferente do escolhido (combinado por telefone, por exemplo), registre a escolha antes, pelo próprio dono na tela, ou ajuste `requested_plan_id` e `requested_cycle` no banco e rode o script.

## Cancelar

```sql
UPDATE subscription s SET status = 'CANCELED'
FROM establishment e WHERE e.organization_id = s.organization_id AND e.slug = 'sakura-sushi';
```

A mesa para na hora (sem tolerância). Para voltar, rode o script de ativação.
