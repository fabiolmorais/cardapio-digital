# 0014 — Resumo diário por produto nos relatórios de períodos longos

**Status:** aceita (25/11/2026). Complementa o ADR 0013. Implementação na etapa C9 do plano de continuação dos relatórios.

## Contexto

O ADR 0013 decidiu somar os relatórios direto nas tabelas da operação. A revisão ficou marcada para quando uma tela passasse de 1 segundo com 12 meses de dados. Com o histórico completo, a Visão geral e a Cozinha chegaram perto desse limite.

Medição com o seed de desenvolvimento (plano Business, 25/11/2026, melhor de 4 chamadas):

| Tela | 30 dias | 12 meses |
| --- | --- | --- |
| Visão geral | 0,49 s | 0,75 s |
| Cozinha e tempos | 0,15 s | 0,68 s |
| Cardápio | 0,24 s | 0,98 s |

Nenhuma tela passou de 1 segundo. Em cada uma, porém, o tempo vem quase todo de uma consulta sobre os itens dos pedidos:

- na Visão geral, os itens mais vendidos (0,41 s com 12 meses);
- na Cozinha, a lista dos itens com os horários de cada passo (0,3 s).

O PostgreSQL estima 45 itens por produto e encontra cerca de 4 mil. Por isso escolhe laços aninhados, com uma busca no índice para cada item. O erro de estimativa vem da mistura de restaurantes de tamanhos muito diferentes na mesma tabela, e não se corrige com mais estatística. Desligar os laços aninhados nessa consulta deixa o banco local mais rápido, mas o plano resultante varre as tabelas inteiras de pedidos e itens, de todos os restaurantes. Em produção, isso pioraria a Cozinha de todos.

Os números que se somam por dia (quanto vendeu, quantos preparos, quanto tempo no total) cabem num resumo exato. Outros números precisam de cada item:

- o 9 em 10 (percentil 90);
- o pior horário;
- os picos de itens em preparo;
- os atrasos no passe.

Resumidos, esses viram aproximações por faixa de tempo. O Cardápio também fica de fora: as duas consultas que pesam nele (o interesse pelo cardápio e os pedidos juntos) cruzam celulares e pares de produtos, e isso não cabe num resumo por dia sem perder exatidão.

## Decisão

1. **Uma tabela de resumo por dia, setor e produto (`report_product_day`)**, com quatro somas:
   - **Vendidos e faturamento:** itens não cancelados e fora do rodízio das comandas fechadas. Contam no dia em que a comanda fechou e no setor da mesa da comanda, a mesma regra dos itens mais vendidos.
   - **Preparos e tempo total, em milissegundos:** itens não cancelados que ficaram prontos. Contam no dia em que foram pedidos e no setor da mesa do pedido, a mesma regra do preparo médio.

   O "dia" é o do restaurante, no fuso dele. A tabela `report_summary_state` guarda até onde o resumo foi: os dias antes de `summarized_until` estão nele.
2. **Só onde o número continua exato.** O resumo atende:
   - os itens mais vendidos da Visão geral;
   - o preparo médio por produto, usado nos pontos de atenção da Visão geral e no preparo médio da comparação entre unidades.

   O resto continua nas consultas diretas do ADR 0013, inclusive a tela Cozinha e tempos, cujos blocos precisam de cada item.
3. **Como a leitura junta as partes.** O resumo entra só com dias inteiros já resumidos. O resto do período vai para as consultas diretas: o começo e o fim quando não são dias inteiros (por exemplo, a comparação de "Hoje", que para na mesma hora) e os dias que o resumo ainda não tem, sempre inclusive hoje. As duas partes se somam por produto, e a ordem e a média saem da soma, como nas consultas diretas.
4. **Quem grava.** Uma tarefa de hora em hora vê, para cada restaurante, se o dia virou no fuso dele desde a última volta. Se virou, refaz os 3 dias anteriores ao último resumido e resume até ontem, numa transação por restaurante. Na primeira vez, resume o histórico inteiro. Os 3 dias cobrem o item que fica pronto depois da meia-noite e os ajustes do dia seguinte. Dias mais antigos não mudam: a comanda fechada não volta a abrir.
5. **Mesa que muda de setor.** O resumo guarda o setor e as consultas diretas usam o setor de hoje da mesa. Por isso, mudar uma mesa de setor apaga o resumo do restaurante, e a próxima volta da tarefa refaz tudo. Até lá, os relatórios leem direto.
6. **Separação entre restaurantes.** Toda leitura e toda gravação do resumo filtram por `establishment_id`, que vem do usuário autenticado, como no ADR 0013. O teste de integração prova que o resumo de um restaurante não entra nos números de outro.
7. **Outros ganhos medidos e mantidos.** O cálculo dos picos da cozinha passou a ordenar números em vez de objetos e caiu de um terço para 2% das amostras de CPU da tela Cozinha. O resultado é o mesmo da versão anterior, testado com 20 mil itens aleatórios e com a virada do horário de verão.

## Medição depois

Mesmo seed, mesmo plano, melhor de 4 chamadas, com o resumo feito até ontem:

| Tela | 30 dias | 12 meses |
| --- | --- | --- |
| Visão geral | 0,24 s (era 0,49 s) | 0,39 s (era 0,75 s) |
| Cozinha e tempos | 0,13 s (era 0,15 s) | 0,70 s (era 0,68 s; dentro da variação entre as medições) |

A Cozinha não mudou porque o tempo dela está na consulta da lista de itens, e o resumo não a substitui. O cálculo dos picos ficou mais leve, mas o tempo total da tela vem da espera pelo banco.

## Consequências

- Os números da Visão geral e da comparação entre unidades continuam iguais aos das consultas diretas. O teste de integração compara as duas leituras, com e sem filtro de setor.
- Uma correção feita mais de 3 dias depois do dia, numa comanda já fechada, não entra no resumo. Hoje o sistema não tem essa correção. Se ela surgir, ela precisa apagar o resumo do restaurante, como a troca de setor da mesa.
- O resumo é uma tabela derivada. Os scripts de LGPD não precisam dela: ela guarda só somas por produto e setor, sem dados pessoais.
- Se a Cozinha ou o Cardápio passarem de 1 segundo, o caminho é outro ADR: um resumo próprio para cada bloco, ou números aproximados, decididos tela a tela.
