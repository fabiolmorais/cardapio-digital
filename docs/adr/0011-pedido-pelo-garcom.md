# 0011 — Pedido lançado pelo garçom, com trava de duplicados

**Status:** aceita (18/11/2026). Implementação na fase 2 (RF43).

## Contexto

Conversando com restaurantes, apareceu uma dor: parte dos clientes tem preguiça de pedir pelo celular e prefere falar com o garçom. Hoje o pedido só nasce numa sessão de celular da mesa (`customer_order.session_id` é obrigatório), então esse cliente fica fora do sistema: o garçom anota à parte, e a comanda e a cozinha não ficam sabendo.

Com o garçom pedindo, surge um risco novo. O cliente pede a um garçom e, logo depois, repete o pedido a outro; ou pede pelo celular e também fala com o garçom. O resultado é comida feita em dobro e discussão na hora da conta.

## Decisão

1. **O garçom lança pedidos na comanda da mesa**, pelo painel, mesmo sem nenhum celular na mesa. Se não houver comanda aberta, ela abre nesse pedido.
2. **O restaurante decide.** `order_settings.staff_ordering_enabled`, alterado só por dono e gerente, começa desligado e é independente do pedido pelo celular. Ligado, podem lançar `OWNER`, `MANAGER` e `STAFF`; `KITCHEN` não. Disponível em todos os planos.
3. **O pedido tem uma origem**: um celular (`session_id`) ou uma pessoa da equipe (`placed_by`), nunca os dois nem nenhum, garantido por `CHECK` no banco e, no domínio, por um tipo fechado `OrderOrigin`.
4. **O pedido do garçom vai direto para a cozinha.** Confirmação, limites de valor e de itens, trava de presença e limite de envios por minuto protegem o pedido do cliente anônimo e não se aplicam aqui. Assinatura vencida, produto indisponível e regras de opções valem igual.
5. **A trava de duplicados avisa e exige uma decisão; não proíbe.** Pedir de novo o mesmo item é legítimo, então um bloqueio definitivo atrapalharia o serviço. O envio é recusado com **409** e a lista do que se repete quando o mesmo produto já está na comanda e:
   - ainda não foi entregue; ou
   - foi pedido há menos de 5 minutos, mesmo já entregue.

   A comparação é só pelo produto, sem olhar observação nem opções, porque cada garçom anota de um jeito. Vale contra pedidos de outro garçom e do celular. Item cancelado e pedido recusado não contam. O garçom tira o item ou confirma; a confirmação vale para os produtos daquele aviso, e fica registrada no pedido (`duplicate_confirmed`).
6. **Corrida e repetição de envio.** A conferência acontece com a comanda travada na transação, para que dois envios simultâneos não passem juntos. Cada envio traz um `client_request_id`, único por restaurante: repetir por falha de rede devolve o mesmo pedido.
7. **Rodízio.** O garçom pode lançar rodadas, com os limites do rodízio. Ele não depende da participação de um celular, e a trava de duplicados não se aplica às rodadas, onde repetir é o normal.
8. **Tela enxuta.** Sem vídeos e sem fotos: lista com busca e categorias, nome e preço. O que já está a caminho aparece no topo e como etiqueta no produto, para evitar o duplicado antes do envio.
9. **O que o cliente vê.** Os itens lançados pelo garçom aparecem em "Meus pedidos" e na conta, mas o cliente não os cancela pelo celular. A visita passa a incluir os pedidos do garçom feitos até 2 horas antes da primeira sessão ativa (a mesma validade da sessão), para o cliente que escaneia depois ver o que já pediu, sem revelar a comanda esquecida de outro dia.

## Consequências

- `customer_order.session_id` passa a aceitar nulo: todo código que o lê precisa tratar a origem (cancelamento pelo cliente, divisão por celular no histórico, contagem de envios).
- O cálculo de preço e a montagem dos itens são compartilhados entre o pedido do celular e o do garçom, para as regras não divergirem.
- Alarmes falsos podem acontecer (dois chopps pedidos de propósito em sequência). Custam um toque, bem menos que um prato feito em dobro; a janela de 5 minutos pode ser ajustada com o uso.
- Sem permissão por garçom nesta versão: a opção vale para toda a equipe de salão.
