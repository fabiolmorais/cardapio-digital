# 0010 — Opções dos produtos: um modelo genérico, e não um recurso de pizza

**Status:** aceita (13/11/2026). Implementação na fase 2 (RF27 e RF42).

## Contexto

A dúvida veio da pizzaria: o cliente quer pedir uma pizza meio calabresa, meio frango, e o preço depende dos sabores (em geral, cobra-se o mais caro). Hoje cada item do pedido é um produto com quantidade e observação. O único jeito é escrever os sabores na observação, com preço fixo e sem nenhuma conferência.

O OhChefe atende qualquer restaurante ou bar. As mesmas necessidades aparecem com outros nomes em outros nichos:
- o ponto da carne na hamburgueria e na churrascaria;
- os adicionais pagos ("2× bacon");
- os complementos do açaí (3 grátis, os outros pagos);
- o tamanho da bebida, a dose ou a garrafa;
- os acompanhamentos do prato executivo ("escolha 2 de 6").

Um recurso só para pizza resolveria um caso e deixaria os outros de fora. Pior: obrigaria quem não é pizzaria a conviver com telas e campos que não fazem sentido para ela.

## Decisão

Um modelo genérico de escolhas no produto, em três peças:

1. **Tamanhos** (`product_variant`): versões do produto com preço, promoção e disponível próprios.
2. **Grupos de opções** (`option_group`): uma pergunta, numa biblioteca do restaurante, ligada a quantos produtos quiser (`product_option_group`).
3. **Opções** (`option_item`): as respostas, cada uma com preço (pode ser zero) e disponível.

O comportamento de cada grupo vem de poucos ajustes que se combinam:

| Ajuste | Para quê |
| --- | --- |
| mínimo e máximo de escolhas | opcional ou obrigatório, e quantas escolhas cabem |
| repetir a mesma opção | "2× bacon" |
| divide o item | as escolhas são partes do mesmo item (½, ⅓, ¼): a pizza de vários sabores |
| como cobra: soma, maior preço ou média | adicionais somam; sabores de pizza cobram o maior preço ou a média |
| escolhas grátis | os complementos do açaí |
| preço por tamanho (`option_item_price`) | o sabor custa mais na pizza grande |
| limite por tamanho (`product_option_group_limit`) | broto com 1 sabor, grande com até 3 |

A pizza meio a meio não é um tipo especial: é um produto com tamanhos e um grupo que **divide o item** e **cobra pelo maior preço**.

Regras que valem para todos:

- **O back-end calcula o preço** do item (base do tamanho, com a promoção, mais a parte de cada grupo). O celular só mostra a conta; o servidor confere de novo, como já faz com o preço promocional.
- **Tudo é copiado no pedido** (`order_item_option`): os nomes, os títulos e os preços de cada escolha. Mudar o cardápio depois não altera pedidos feitos.
- **Produto sem tamanhos nem opções funciona exatamente como hoje**, com o "Adicionar" de um toque. Nas telas da gestão, a parte de opções fica recolhida até o restaurante querer usar.
- **Modelos prontos** ("Ponto da carne", "Pizza: sabores meio a meio", "Adicionais", "Complementos do açaí"…) para começar sem entender a configuração.
- **No rodízio**, as escolhas que definem o item aparecem sem preço. Os adicionais pagos ficam de fora, porque o rodízio já é cobrado por pessoa.

## Alternativas descartadas

- **Pizza como tipo especial de produto** (com campos de sabores e tamanhos só dela): resolve um nicho só. Exigiria outros tipos especiais para hambúrguer, açaí e bebidas, cada um com as suas telas e regras.
- **Variações como combinações prontas** (cadastrar "Pizza Grande Calabresa/Frango" como um produto): explode em centenas de produtos e não cobre os adicionais.
- **Só a observação livre**: não confere nada nem calcula o preço, e a cozinha depende do que o cliente escreveu.

## Consequências

- Seis tabelas novas e dois campos em `order_item`, todas com `establishment_id` e índice, como as demais.
- O cálculo do preço vira uma regra de domínio com testes unitários para cada modo de cobrança: soma com escolhas grátis, maior preço, média com arredondamento para cima no centavo, preço e limite por tamanho.
- O detalhe do prato na mesa ganha os blocos de escolha. O JavaScript da página da mesa precisa continuar abaixo de 200 KB.
- A cozinha, o Modo TV, a comanda impressa e o "Por item" passam a mostrar as escolhas embaixo do item. O "Por item" só soma itens iguais em produto, tamanho e escolhas.
- O ponto do corte no rodízio de passagem continua fixo por enquanto e pode migrar para um grupo de opções depois.
- A modelagem completa (ajustes, fórmula do preço, exemplos por nicho e regras) está na seção "Opções dos produtos" da especificação.
