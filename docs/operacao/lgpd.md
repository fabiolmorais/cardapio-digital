# Pedidos de dados (LGPD)

O titular dos dados (o dono do restaurante, pela empresa) pode pedir uma cópia dos dados e pedir que eles sejam apagados. No piloto, o pedido chega por e-mail ou WhatsApp, e nós atendemos com dois scripts SQL. Uma tela "exportar e excluir a conta" no admin fica para quando houver mais clientes.

Antes de rodar, confirme que quem pede é mesmo o dono: responda pelo e-mail do cadastro da empresa, e não por outro canal.

## Exportar (cópia dos dados)

```bash
psql "$DATABASE_URL" -v slug=sakura-sushi -At -f docs/operacao/exportar-dados.sql > sakura-sushi.json
```

O JSON traz a empresa, o restaurante, a equipe (sem senha, PIN nem tokens), o salão, o cardápio, os rodízios, as comandas com os pedidos e a assinatura. Mande o arquivo para o e-mail do cadastro e apague a cópia local depois.

Fotos e vídeos ficam no armazenamento (R2 e Bunny), fora do banco. Se o titular pedir também os arquivos, baixe a pasta do restaurante no R2.

## Apagar (anonimizar)

Exporte primeiro: depois da anonimização, não há como recuperar os dados.

```bash
psql "$DATABASE_URL" -v slug=sakura-sushi -f docs/operacao/anonimizar-restaurante.sql
```

Comandas, pedidos e chamados são permanentes: eles ficam, mas sem ninguém identificável. O script ([anonimizar-restaurante.sql](anonimizar-restaurante.sql)):

- troca o nome, o documento, o WhatsApp e o e-mail da empresa;
- tira a marca do restaurante e o desativa (os QR Codes param);
- apaga o nome, o e-mail e a senha de quem só trabalha neste restaurante, e desliga a equipe inteira (o PIN fica, só como hash, porque o banco o exige);
- apaga as observações escritas nos pedidos;
- encerra as sessões, desvincula os aparelhos e apaga as inscrições de notificação;
- cancela a assinatura.

A resposta mostra quantos restaurantes e quantas pessoas foram anonimizados (1 e o tamanho da equipe). Depois, apague a pasta do restaurante no R2 e os vídeos no Bunny.

O teste `LgpdScriptsTest` roda os dois arquivos a cada build.
