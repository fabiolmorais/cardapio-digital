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

## Complemento (14/11/2026): opções com foto, descrição e vídeo

### Contexto

Testando a pizza no staging, o jeito das pizzarias de verdade mostrou dois problemas:

- **Os sabores apareciam sem foto.** A escolha era uma lista de botões com nome e preço, e as fotos e os vídeos são a principal característica do OhChefe.
- **O tamanho escondido no produto não é o mais comum.** Na pizzaria de referência, cada tamanho é um prato do cardápio (Pequena, Média e Grande), com foto e "a partir de". Ao tocar no tamanho, abre um segundo cardápio com os sabores, cada um com foto, ingredientes e o preço naquele tamanho, mais a busca pelo nome. A regra ("de 1 a 2 sabores", cobrar pelo maior) é de cada tamanho.

### Decisão

1. **A opção ganha descrição, fotos e vídeos**, com os mesmos limites e o mesmo caminho do prato:
   - até 8 mídias, das quais até 3 vídeos de até 60 s;
   - foto por URL assinada, com os três tamanhos gerados pelo back;
   - vídeo pelo serviço de vídeo, com o aviso do webhook.

   A mídia passa a ter um dono, que é um produto ou uma opção (`product_media.option_item_id`, com exatamente um dos dois preenchido). Vale para qualquer nicho: o adicional com foto, o complemento do açaí, o drinque.
2. **O preço da opção pode ser por produto**, sem tamanho (`option_item_price.product_variant_id` passa a aceitar vazio). Uma biblioteca só de sabores serve à Pequena, à Média e à Grande, cada uma com a sua coluna de preço. Foto, descrição e "esgotado" do sabor são cadastrados uma vez.

   A ordem do preço da opção:
   - o preço no tamanho escolhido;
   - o preço neste produto;
   - o preço da opção.
3. **Na mesa, a escolha vira um cardápio:**
   - a tela cheia mostra a foto do prato no topo;
   - a busca pelo nome vem logo abaixo;
   - cada opção aparece com foto, descrição e preço, e tocar na foto abre a galeria com os vídeos;
   - por fim vêm a observação e o botão com o total.

   Grupo sem nenhuma foto continua como lista simples (o ponto da carne não precisa de imagem).
4. **Os dois jeitos continuam valendo.** Um produto com os tamanhos dentro (o refrigerante de 300 ou 500 ml) ou um produto por tamanho (a pizzaria). O restaurante escolhe.

"Sabores mais pedidos" no topo da escolha fica para a fase 2.

### Consequências

- Uma migração nova muda `option_item` (descrição), `product_media` (dono) e `option_item_price` (tamanho opcional, com unicidade por produto, opção e tamanho).
- Os casos de uso de mídia passam a receber o dono (produto ou opção). A API ganha as mesmas rotas de mídia em `/api/v1/option-items/{id}`. O cardápio público traz a galeria de cada opção.
- O Reels e a capa do prato continuam só com a mídia dos produtos.
- A tela de escolha da mesa fica mais pesada. As fotos usam o tamanho pequeno e carregam sob demanda, e o orçamento de 200 KB de JavaScript continua conferido pelo e2e.
