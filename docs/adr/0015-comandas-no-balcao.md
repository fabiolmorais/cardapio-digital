# 0015 — Comandas no balcão

**Status:** aceita (25/11/2026). Implementação na fase 2 (RF45).

## Contexto

Em muitos bares, parte do público não senta. As pessoas ficam no balcão, conversam com o barman e pedem ali mesmo, cada uma com a sua conta. Hoje o sistema não atende esse cliente, porque supõe "uma comanda aberta por mesa":

- `tab.table_id` é obrigatório;
- o índice único `tab_one_open_per_table_uq` impede duas comandas abertas na mesma mesa;
- a cozinha, a impressão, o painel e os relatórios mostram e somam pela mesa: 11 adaptadores fazem junção com `dining_table`, e os relatórios filtram o setor por ela.

O barman acaba anotando à parte, e a cozinha, a comanda e os relatórios não ficam sabendo.

Havia dois caminhos:

1. Deixar a mesa da comanda vazia para quem está no balcão.
2. Tratar o balcão como um lugar do salão, que aceita várias comandas abertas ao mesmo tempo.

O primeiro parece o mais direto, mas é perigoso: toda consulta que junta a comanda com a mesa passaria a perder as comandas do balcão sem dar erro, inclusive o faturamento dos relatórios. O segundo mantém essas junções funcionando.

## Decisão

1. **O balcão é um lugar do salão.** `dining_table` ganha `kind` (`TABLE` ou `COUNTER`). O restaurante cadastra quantos balcões quiser em "Mesas e QR Codes", cada um com nome e setor.
   - O balcão não tem QR Code nem página para o cliente: o endereço público dele responde 404.
   - Não conta no limite de mesas do plano.
   - Está disponível em todos os planos.
2. **O balcão aceita várias comandas abertas.** A comanda ganha:
   - `at_counter`: a comanda está no balcão;
   - `customer_name`: como a equipe chama o cliente, até 40 letras, obrigatório quando `at_counter`;
   - `card_number`: o número de um cartão físico, opcional, até 10 caracteres.

   "Uma comanda aberta por mesa" passa a valer só para `at_counter = false`. O índice é refeito numa migração nova (a antiga não é editada). Um índice novo impede o mesmo cartão em duas comandas abertas do restaurante. O domínio garante que `at_counter` só existe num lugar do tipo `COUNTER`, e vice-versa.
3. **Quem abre.** `OWNER`, `MANAGER` e `STAFF` abrem a comanda do balcão na hora, sem pedido (`KITCHEN` não). O back-end confere em todo envio, com o teste de acesso entre restaurantes (404). O nome e o cartão podem ser corrigidos com a comanda aberta.
4. **Os pedidos do balcão são lançados pela equipe**, pela tela do pedido pelo garçom (ADR 0011), com a trava de duplicados.
   - No balcão, lançar é sempre permitido, mesmo com `order_settings.staff_ordering_enabled` desligado. Sem isso, o balcão não funciona. Nas mesas, a regra do ADR 0011 continua igual.
   - O lançamento passa a ter como destino a comanda, e não só a mesa. Na mesa, continua abrindo a comanda no primeiro pedido; no balcão, a comanda já existe.
   - O cliente do balcão não pede pelo próprio celular nesta versão. Por isso o balcão não tem sessão de celular, trava de presença, chamados nem avaliação do atendimento.
5. **Como aparece.** Na fila da cozinha, na impressão, no painel e nas notificações, o lugar é **Balcão · João** (com **Cartão 12**, se houver), e não o número de uma mesa. O nome é do lugar cadastrado ("Balcão", "Balcão da varanda").
6. **Pagar e fechar** seguem iguais: receber, pagamento adiantado, taxa de serviço (RF39) e fechar comanda. Na regra "garçom que atendeu", vale quem mais lançou e entregou itens, porque no balcão não há chamados.
7. **Do balcão para uma mesa.** "Mudar de mesa" (ADR 0012) aceita a comanda do balcão:
   - para mesa livre, a comanda vai para a mesa e deixa de ser do balcão (`at_counter = false`). O nome e o cartão continuam nela, e o histórico de mudanças registra a saída do balcão;
   - para mesa ocupada, as comandas se juntam como hoje, na comanda da mesa.

   O destino é sempre uma mesa nesta versão: da mesa para o balcão e de uma comanda do balcão para outra ficam para depois. O rodízio não abre no balcão, porque ele é por pessoas na mesa.
8. **Relatórios.**
   - O faturamento, as vendas por setor, o cardápio, a cozinha e a equipe contam o balcão normalmente, porque ele é um lugar do setor.
   - Em "Mesas e ocupação", o balcão fica fora da contagem de mesas, do giro, do pico, da ocupação por hora e do faturamento por hora de mesa ocupada: ele não tem a ideia de mesa ocupada. Em "Mesas que mais vendem", cada balcão aparece como uma linha.
   - Em "Celular e rodízio", as comandas do balcão ficam fora da conta "do QR ao pedido", porque nunca têm QR.
9. **Tempo real.** Os eventos mínimos de sempre (comanda aberta ou mudou: ids e status), no canal do restaurante. Nada vai para canais de mesa, porque o balcão não tem celulares.

## Consequências

- O código que supõe uma comanda por mesa precisa olhar a comanda, e não a mesa, quando o lugar é um balcão. Os pontos principais: o mapa do salão e o painel (um cartão por balcão, com quantas comandas abertas), "Comandas abertas", o detalhe da comanda e o lançamento pela equipe.
- As consultas de ocupação e do funil do celular passam a filtrar `kind = 'TABLE'`. Os outros relatórios não mudam.
- O nome do cliente é um dado pessoal, ainda que informal. Ele fica só na comanda, nunca vai para os logs, entra na exportação de dados do restaurante e é tratado pela anonimização da LGPD (`customer_name` e `card_number` apagados).
- O limite de mesas do plano passa a contar só `kind = 'TABLE'`.
- Sem o celular do cliente, o balcão depende do barman para tudo. Se os clientes pedirem, a próxima versão pode dar a cada comanda do balcão um QR próprio (a sessão do celular passaria a ser da comanda, e não da mesa).
