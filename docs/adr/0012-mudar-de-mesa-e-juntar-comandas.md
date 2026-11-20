# 0012 — Mudar a comanda de mesa e juntar comandas

**Status:** aceita (19/11/2026). Implementação na fase 2 (RF44).

## Contexto

Em bar é comum o grupo mudar de mesa (sair do sol, ir para uma mesa maior) ou juntar mesas quando chegam amigos. Hoje a comanda fica presa à mesa em que abriu (`tab.table_id`, uma comanda aberta por mesa), e o mesmo vale para as sessões dos celulares (`table_session.table_id`), os chamados (`call.table_id`) e o rótulo da mesa na cozinha e na impressão. Sem uma forma de mudar, o garçom fecha uma conta que não foi paga, ou deixa a comanda na mesa errada: a cozinha entrega o prato no lugar errado e a mesa nova aparece livre no painel.

## Decisão

1. **Uma ação só: "Mudar de mesa".** O destino é qualquer mesa ativa do restaurante, de qualquer setor. Se a mesa de destino está livre, a comanda **muda de mesa**. Se já tem comanda aberta, as duas **se juntam** numa só, depois de uma confirmação que mostra as duas mesas e os dois totais.
2. **Quem faz.** `OWNER`, `MANAGER` e `STAFF`, sempre ligado e em todos os planos; `KITCHEN` não. Não depende da assinatura estar em dia, porque não é um serviço da mesa ao cliente. O back confere em todo envio, com o teste de acesso entre restaurantes (404).
3. **Mudar para mesa livre.** A comanda guarda o id e passa a apontar para a mesa nova; a mesa antiga fica livre na hora. Pedidos, pagamentos adiantados, rodízio e itens a caminho vão junto, porque já pertencem à comanda.
4. **Juntar.** A comanda de destino continua (mantém o id e a mesa); a de origem recebe os pedidos e os pagamentos e é encerrada com o status novo `MERGED` e `merged_into_tab_id`, sem total. Nada é apagado. A comanda resultante abre na data da mais antiga das duas. Comanda `MERGED` não entra no histórico de comandas fechadas nem em relatórios: o que ela tinha é contado uma vez, na de destino.
5. **Rodízio ao juntar.**
   - Só uma das duas tem rodízio: a comanda junta fica com ele. Os celulares da outra mesa entram como "quem chegou depois" (o convite "Esta mesa está no rodízio…" e o garçom ajusta as pessoas), o fluxo que já existe.
   - As duas no **mesmo rodízio**: as pessoas da origem são somadas ao rodízio do destino, cada faixa com o preço que já tinha sido copiado na ativação (ninguém paga diferente por causa da junção). Os itens de rodada e os celulares participantes passam para o rodízio do destino.
   - As duas em **rodízios diferentes**: a junção é recusada (422), com a orientação de fechar uma delas antes. Uma comanda não tem dois rodízios.
6. **Os celulares vão junto.** As sessões ativas da mesa de origem passam para a mesa de destino, com a presença já validada e a mesma validade. Cada celular recebe pelo canal da sessão o aviso de que a mesa mudou e passa a abrir a página da mesa nova, sem escanear de novo. Um celular que veio na mudança e abre a mesa antiga é levado para a nova. Quem escanear a mesa antiga depois começa uma visita nova.
7. **Os chamados abertos vão junto**, para o garçom ir à mesa certa. Ao juntar, dois chamados abertos do mesmo tipo viram um só (fica o mais antigo).
8. **Cozinha e impressão.** Os pedidos vão junto com a comanda, inclusive a mesa deles (`customer_order.table_id`): a fila da cozinha, a impressão e os avisos dos itens já saem para a mesa nova sem mudar essas partes. Onde cada pedido foi feito fica no histórico de mudanças. Se havia itens a caminho já impressos, a estação imprime uma via curta "MUDOU DE MESA: 04 → 12" (ou "JUNTOU: 04 → 12") com esses itens, para quem leva o prato não ir à mesa errada.
9. **Histórico.** Cada mudança fica registrada (`tab_move`: comanda, mesa de origem, mesa de destino, comanda juntada, quem fez, quando) e aparece no detalhe da comanda: "Mudou da Mesa 04 para a Mesa 12 às 21:40, por Carlos".
10. **Corrida.** A troca trava, numa transação, as comandas envolvidas (sempre na mesma ordem, para não haver impasse) e confere de novo a mesa de destino. Se outro garçom abriu ou mudou uma comanda no meio do caminho, o envio é recusado com **409** e o painel pede para escolher de novo.
11. **Tempo real.** Evento mínimo para a equipe (comanda mudou: ids das mesas e das comandas) e para os celulares envolvidos (a mesa mudou), conforme a regra dos canais: nada de um restaurante no canal de outro.

## Consequências

- `tab.status` ganha `MERGED`, e todo código que lê comandas fechadas ou abertas precisa continuar filtrando por status (hoje já filtra por `OPEN`/`CLOSING`/`CLOSED`).
- `customer_order.tab_id` e `table_id`, `tab_payment.tab_id`, `table_session.table_id`, `call.table_id`, o rodízio (`tab_rodizio.tab_id`, `order_item.tab_rodizio_id`, `rodizio_phone`) passam a ser atualizados na troca, numa transação. A mesa do pedido acompanha a comanda; onde ele foi pedido de fato fica no `tab_move`.
- A regra "Meus pedidos mostra a visita atual" continua funcionando, porque as sessões mudam de mesa sem perder o início. Depois de juntar, os celulares das duas mesas veem os pedidos das duas: viraram um grupo só.
- A junção de dois rodízios iguais só acontece se as faixas com gente têm o mesmo preço nas duas comandas (ativadas no mesmo preço do dia e horário); senão, a junção é recusada (422), porque uma pessoa pagaria um preço diferente do que tinha.
- Não há desfazer nesta versão. Juntar por engano mistura duas contas, por isso a confirmação mostra as duas mesas e os dois totais. Separar uma parte da comanda (dois foram para outra mesa) e dividir a conta ficam para depois.
