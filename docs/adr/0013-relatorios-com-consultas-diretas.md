# 0013 — Relatórios com consultas diretas nas tabelas da operação

**Status:** aceita (21/11/2026). Implementação na fase 2 (RF24, RF29), conforme o plano dos relatórios (etapas R0 a R14). Complementada pelo ADR 0014 (resumo diário por produto nos períodos longos).

## Contexto

A fase 2 começa pelos relatórios: oito telas no desenho (14.1 a 14.8) mais as telas de plano, estado, celular e configuração (14.9 a 14.15), cobrindo a lista REL-01 a REL-84. Quase tudo usa o que o sistema já grava: comandas (`tab`, com `closed_at`, `total_cents`, `payment_method` e `closed_by`), itens (`order_item`, com o horário de cada passo, quem entregou e quem cancelou), chamados (`call`, com quem aceitou), pagamentos (`tab_payment`), sessões dos celulares (`table_session`, com a forma de presença), mudanças de mesa (`tab_move`) e o rodízio.

Havia três caminhos:

1. Consultas diretas nessas tabelas, somando na hora.
2. Tabelas de resumo por dia, preenchidas por uma tarefa agendada ou por eventos.
3. Uma réplica de leitura ou um banco analítico separado.

O volume de um restaurante é pequeno: um grande fecha perto de 100 comandas por dia, cerca de 36 mil por ano, com algumas centenas de milhares de itens. Somas por período, com índice em `establishment_id` e na data, respondem em milissegundos no PostgreSQL. As tabelas de resumo trariam outro problema: um número que muda depois do fato (item cancelado, comanda juntada, pagamento corrigido) precisaria refazer o resumo, e o relatório poderia discordar do "Faturado hoje" do painel.

## Decisão

1. **Consultas diretas nas tabelas da operação**, sem tabela de resumo e sem réplica. Se uma tela passar de 1 segundo com 12 meses de dados (medido com o seed de desenvolvimento), cria-se um resumo por dia só para aquela consulta, num ADR novo.
2. **Hexagonal, só leitura:**
   - `domain/report`:
     - `ReportPeriod`, um record com início, fim e fuso do restaurante, que calcula o período anterior de mesmo tamanho e o mesmo dia da semana anterior;
     - `ReportAccess`, com o que o plano libera;
     - as regras de cálculo puras: curva ABC, percentil 90 ("9 em 10 até"), previsão do dia e variação.
   - `application/port/in/report`: um caso de uso por tela.
   - `application/port/out/report`: portas de leitura por área.
   - `adapter/out/persistence/report`: SQL nativo **parametrizado** (`JdbcClient` ou `@Query(nativeQuery = true)`), devolvendo projeções, nunca entidades JPA.
3. **Separação entre restaurantes:**
   - toda consulta recebe e filtra por `establishment_id`, vindo do usuário autenticado;
   - o filtro do Hibernate não alcança SQL nativo, então a defesa em dobro aqui é o teste: cada endpoint tem um teste de integração com dois restaurantes provando que os números de um nunca aparecem no outro.
4. **Regras de cálculo comuns a todas as telas:**
   - Faturamento: soma de `tab.total_cents` das comandas `CLOSED`, pelo `closed_at`. É a mesma regra do "Faturado hoje" do painel.
   - A taxa de serviço fica fora do faturamento.
   - Itens cancelados ficam fora do faturamento e das quantidades vendidas; contam no valor cancelado.
   - Comanda `MERGED` não conta; o que ela tinha conta uma vez, na comanda de destino (ADR 0012).
   - O "dia" é o dia do restaurante, no fuso dele (`establishment.time_zone`); o banco continua em UTC.
   - A comparação é com o período anterior de mesmo tamanho. Em "Hoje", é com o mesmo dia da semana anterior, até a mesma hora.
   - O histórico consultável segue o plano: 12 meses no Pro, ilimitado no Business e no Enterprise.
5. **Plano no back-end:**
   - Cada resposta traz o acesso (`BASIC` ou `FULL`).
   - Os blocos completos vão `null` para quem só tem `BASIC_REPORTS`; o front mostra o bloco bloqueado, mas o dado nunca sai do servidor.
   - Sem `BASIC_REPORTS` nem `REPORTS`, os endpoints respondem 403 com Problem Details. A exportação exige `REPORT_EXPORT`.
   - Só `OWNER` e `MANAGER` acessam.
6. **API:**
   - `GET /api/v1/reports/{tela}?from=&to=&sectorId=`, uma chamada por tela, com os blocos da tela;
   - listas longas, como a tabela de produtos, em endpoints paginados;
   - exportação em `GET /api/v1/reports/{tela}/export?format=csv`.
7. **Índices:** uma migração nova com os índices compostos que o `EXPLAIN` das consultas pedir, começando por `tab (establishment_id, closed_at)`, `order_item (establishment_id, created_at)` e `call (establishment_id, created_at)`.

## Consequências

- O relatório nunca discorda do painel: as duas telas leem as mesmas linhas.
- Uma correção (item cancelado depois, comanda juntada) aparece no relatório na hora, sem reprocessar nada.
- As consultas ficam em SQL nativo, mais longo de escrever e revisar que JPQL. Cada uma tem teste de integração com dados conhecidos (Testcontainers) e roda só com parâmetros.
- Relatórios pesados competem com a operação no mesmo banco. Com o volume esperado isso não pesa, e o limite de 1 segundo por tela é o gatilho para rever a decisão.
- Os dados que ainda não existem (histórico de promoção, turnos, viradas do cartão, avisos de pedido repetido) precisam começar a ser gravados cedo, porque o histórico só existe a partir do dia em que se começa a guardar.
