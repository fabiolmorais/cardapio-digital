# Referência — Chamaii (produto concorrente)

21/10/2026 · Fabio Leite Morais

> Este arquivo descreve **apenas o Chamaii**, o produto que serviu de inspiração. Nada aqui é requisito do nosso sistema. A especificação do nosso sistema está em [especificacao-cardapio-digital.md](especificacao-cardapio-digital.md).

## Visão geral

O [Chamaii](https://chamaii.com.br/) é um SaaS brasileiro que liga a mesa do cliente à equipe do salão: o cliente escaneia um QR Code (ou encosta o celular num NFC) e chama o garçom, vê o cardápio ou pede a conta, sem app e sem login. A equipe recebe o chamado na hora, com alerta sonoro, no celular e numa TV do salão. Slogan: "Escaneou. Chamou. Chegou."

**Problema que resolve.** Cliente esperando para pedir, acenando para chamar alguém e esperando a conta; garçom fazendo viagens perdidas até a mesa (inclusive para saber qual maquininha levar).

**Público-alvo.** Restaurantes, bares, pubs, cafeterias, rodízios, churrascarias, praças de alimentação, hotéis, pousadas e áreas VIP — de 20 a mais de 100 mesas.

**Três usuários do sistema:**

| Usuário | Onde usa | O que faz |
| --- | --- | --- |
| Cliente da mesa | Navegador do celular (via QR/NFC) | Chama garçom, vê cardápio, pede a conta |
| Equipe (garçom, gerente, dono) | Celular do garçom, TV do salão, painel web | Recebe e atende chamados, gerencia cardápio, mesas e relatórios |
| Parceiro / revendedor | Portal do parceiro | Indica estabelecimentos e recebe 25% de comissão recorrente |

O modelo de negócio é assinatura mensal ou anual por estabelecimento, com 10 dias de teste grátis sem cartão.

## O que o vídeo mostra

O vídeo (29 s, gravado no "Bar do Rubinho", arquivo `contexto/video-cardapio.mp4`) mostra o cliente na mesa usando só o celular, sem baixar app e sem login. A experiência tem três ações: chamar o garçom, ver o cardápio e pedir a conta.

1. **QR code na mesa** — um display de acrílico com o logo do bar, o QR code e o número da mesa ("01").
2. **Leitura pela câmera** do celular, que abre uma página web.
3. **Tela inicial da mesa** — logo e nome do bar, selo "Mesa 01", selo verde "Presença confirmada", título "Como podemos ajudar?" e o texto "Toque no botão abaixo para chamar nossa equipe diretamente até sua mesa".
    1. Botão principal laranja: **Chamar garçom** (rotulado "Ação principal").
    2. Dois cards: **Ver cardápio** ("Fotos e preços") e **Pedir a conta** ("Pagamento na mesa").
    3. Links para **Instagram** e **Avalie no Google**.
4. **Cardápio** — link "Voltar para a mesa", contador de itens, busca "Buscar no cardápio…", abas de categoria (Todos, Destaques, Mexidão, Espetinhos…).
    1. Cada item tem foto, nome, descrição curta, preço antigo riscado e preço promocional (ex.: Espetinho Picanha completo, de R$ 39,90 por R$ 36,00).
    2. Itens podem aparecer como **Indisponível**, com a foto em cinza.
5. **Reels do prato** — ao tocar num item, abre uma tela cheia vertical no estilo Stories/Reels.
    1. Várias mídias por prato ("1 de 3", "4 de 5"), incluindo vídeo com botão "Mudo".
    2. Selos "Reels do prato", "Destaque" e "Indisponível no momento"; categoria do prato (ex.: "Carne").
    3. Nome, preço, preço antigo, descrição com "leia mais", "Toque para pausar" e "Deslize a tela para o próximo prato".
6. **Chamar garçom** — o botão vira o card "Aguardando atendimento · Garçom chamado! Já notificamos a equipe. Alguém irá até sua mesa em instantes", com animação de confete e o botão **Cancelar chamado**.

O cliente não faz pedido pelo celular no vídeo: o cardápio só mostra pratos, e o pedido continua sendo feito ao garçom.

## Funcionalidades

A plataforma tem seis módulos: app da mesa, cardápio digital, painel da equipe/Modo TV, gestão do estabelecimento, conta e assinatura, e programa de parceiros. Fontes: [home](https://chamaii.com.br/), [recursos](https://chamaii.com.br/recursos), [central de atendimento](https://chamaii.com.br/atendimento), [parceiros](https://chamaii.com.br/parceiros), [cadastro](https://chamaii.com.br/cadastro) e [sobre](https://chamaii.com.br/sobre).

### 1. App da mesa (cliente)

- Acesso por **QR Code** individual por mesa ou **tag NFC** no display; abre no navegador, sem app, conta ou login.
- Identificação automática da mesa (sem digitar número).
- **Validação de presença física** por Wi-Fi/IP do estabelecimento e geolocalização GPS; só quem está no salão consegue chamar (selo "Presença confirmada").
- **Chamar garçom** em 1 toque, com confirmação imediata e opção de cancelar.
- **Pedir a conta** com escolha de forma de pagamento (Pix ou cartão) — avisa o garçom qual maquininha levar.
- Notificação de volta ao cliente: "Garçom Lucas a caminho da mesa".
- **Botões personalizados** adicionais (2 no Essencial, ilimitados nos outros) e links para Instagram e "Avalie no Google".
- Marca do estabelecimento: logo, cor principal e cor secundária.

### 2. Cardápio digital

- Produtos e categorias ilimitados; busca; aba "Destaques".
- Fotos em alta resolução e **vídeos curtos ("Reels do prato")** em tela cheia, com navegação por deslize.
- Preço, preço promocional ("de/por"), itens sazonais e status **indisponível**.
- Atualização em tempo real, sem reimpressão.
- O cardápio é só vitrine: não há carrinho nem pedido pelo celular.

### 3. Painel da equipe e Modo TV

- Chamados em tempo real, ilimitados, em **fila por ordem de espera** com cronômetro ("espera 00:24s").
- **Alerta sonoro** contínuo e configurável, no celular do garçom e na TV.
- Estados do chamado: Pendente/Aguardando → Em atendimento (com nome do garçom) → Atendido (com tempo, ex.: "Atendido em 15s").
- **Modo TV / KDS**: painel de alta legibilidade para Smart TV na cozinha, copa ou balcão.
- **Visão do garçom** no celular: status online, fila atual, "Toque para atender".
- Login individual do garçom por **PIN**, com histórico de quem atendeu.
- Mesas agrupadas por **setor/ambiente** (ex.: "Salão Principal", "Deck").

### 4. Gestão do estabelecimento

- Cadastro de mesas e geração de **display para impressão (PNG/SVG)** com QR Code.
- Equipe com **perfis Owner, Manager e Staff** (Proprietário, Gerente, Garçom); auditoria de acessos no Business.
- **Relatórios**: tempo médio de espera e resposta, horários de pico, desempenho por mesa e por colaborador, volume por tipo de chamado.
- Histórico de chamados por 30 dias, 12 meses ou ilimitado, conforme o plano; exportação CSV/Excel/PDF.
- Múltiplas unidades e redes (Enterprise).

### 5. Conta, assinatura e suporte

- Cadastro self-service: nome, estabelecimento, CPF/CNPJ, WhatsApp, e-mail, senha, plano do teste e código de indicação.
- Teste de 10 dias com recursos liberados na hora, sem cartão.
- Upgrade, downgrade e cancelamento pelo painel; cobrança mensal ou anual.
- Central de atendimento com formulário de chamado (categoria, urgência, reCAPTCHA), contato comercial e página de status (meta de 99,9% de disponibilidade).

### 6. Programa de parceiros

- Inscrição com CPF/CNPJ, tipo de atuação (comercial, indicador, implantação, agência) e segmentos; aprovação manual em até 24 h.
- Link exclusivo (`/p/seu-nome`) e código de indicação; atribuição "First-Valid-Partner".
- **25% de comissão recorrente** sobre mensalidades pagas, repasse via Pix todo dia 10, saque mínimo de R$ 100.
- Portal do parceiro com clientes, oportunidades e comissões; autoindicação proibida.

## Planos e preços

São três planos por número de mesas, de R$ 59,90 a R$ 199,90 por mês, mais um Enterprise sob consulta. O anual sai 10 meses pelo preço de 12 ("economize 17%"). Valores da API pública do site (`/api/public/plans`) e da [tabela comparativa](https://chamaii.com.br/recursos), consultados em 21/10/2026.

| Recurso | Essencial | Pro (mais escolhido) | Business | Enterprise |
| --- | --- | --- | --- | --- |
| Preço mensal | R$ 59,90 | R$ 99,90 | R$ 199,90 | Sob consulta |
| Preço anual | R$ 599 (R$ 49,92/mês) | R$ 999 (R$ 83,25/mês) | R$ 1.999 (R$ 166,58/mês) | Sob consulta |
| Mesas simultâneas | 20 | 50 | 100 | Ilimitadas |
| Usuários da equipe | 5 | Ilimitados | Ilimitados | Ilimitados |
| Botões personalizados | 2 | Ilimitados | Ilimitados | Ilimitados |
| Vídeos nos pratos | Não | Sim | Sim | Sim |
| Permissões por perfil | Não | Sim | Sim | Sim |
| Cor secundária e display personalizado | Não | Sim | Sim | Sim |
| Histórico de chamados | 30 dias | 12 meses | Ilimitado | Ilimitado |
| Relatórios avançados | Não | Sim | Sim | Sim |
| Exportação CSV/Excel/PDF | Não | Não | Sim | Sim |
| Múltiplos setores e ambientes | Não | Não | Sim | Sim |
| Auditoria de acessos | Não | Não | Sim | Sim |
| Múltiplas unidades / redes | Não | Não | Não | Sim |
| Suporte | E-mail, horário comercial | WhatsApp prioritário | WhatsApp prioridade máxima | Gerente dedicado 24 h, SLA |

Comum a todos: QR Code e NFC por mesa, display para impressão, chamados ilimitados, chamar garçom e pedir a conta, alertas sonoros, painel KDS/Modo TV, validação por Wi-Fi/IP, cardápio com fotos, produtos ilimitados, logo e cor principal, relatórios básicos.

**Como o Chamaii modela os planos.** A API expõe cada plano como limites numéricos (`maxTables`, `maxUsers`, `maxCustomButtons`, `historyDays`, com 0 = ilimitado) mais um mapa de *entitlements* booleanos (`productVideos`, `rolePermissions`, `reportExport`, `multiEnvironment`, `wifiValidation`…). Adotamos o mesmo desenho na nossa modelagem.

## Detalhes técnicos observados

- Site feito em **Next.js**, renderizado no navegador.
- Lista de planos servida por uma API pública (`/api/public/plans`).
- Prioridade de suporte e disponibilidade (99,9%) anunciadas na central de atendimento.
