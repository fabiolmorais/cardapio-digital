# Roadmap do MVP

21/10/2026 · Fabio Leite Morais

Plano passo a passo, do primeiro commit até o MVP validado num restaurante piloto. Stack: Spring Boot 4 + Next.js + PostgreSQL ([especificação](especificacao-cardapio-digital.md), seção "Arquitetura e stack"). As horas vêm de [custos-e-prazos.md](custos-e-prazos.md).

## Resumo

São **16 etapas** e **5 marcos**, em cerca de **24 semanas** com dedicação de 30 horas produtivas por semana (cerca de 705 horas no cenário realista; em meio período, cerca de 47 semanas).

A ordem segue uma regra: cada etapa entrega algo que dá para usar e mostrar. O primeiro marco (semana 12) já reproduz o vídeo do Chamaii, com a mesa chamando o garçom em tempo real. Depois vêm pedidos, vídeos e rodízio.

A busca pelo restaurante piloto começa só depois que o MVP estiver pronto (Marco 4). O tempo até fechar com um restaurante não está nas semanas abaixo; a semana do Marco 5 conta como se o piloto começasse logo em seguida.

| # | Etapa | Horas (otimista) | Horas (realista, +30%) | Termina por volta da semana | Requisitos |
| --- | --- | --- | --- | --- | --- |
| 0 | Preparação (fora do código) | — | — | antes de começar | — |
| 1 | Fundação técnica | 36 | 47 | 2 | — |
| 2 | Contas, login e equipe | 58 | 75 | 5 | RF01, RF02 |
| 3 | Salão: setores, mesas e QR Code | 32 | 42 | 6 | RF03, RF04 |
| 4 | Cardápio e fotos | 44 | 57 | 8 | RF11, RF12 |
| 5 | Página da mesa (vitrine) | 32 | 42 | 9 | RF05 |
| 6 | Chamados em tempo real, painel e Modo TV | 52 | 68 | 12 | RF06, RF08, RF09, RF10 |
| | **Marco 1 — A mesa chama o garçom** | | | **12** | |
| 7 | Trava de presença | 24 | 31 | 13 | RF19 |
| 8 | Pedidos, comanda e cozinha | 58 | 75 | 15 | RF13, RF14, RF17, RF18 |
| 9 | "A caminho", histórico e fechar a conta | 12 | 16 | 16 | RF07, RF15, RF16 |
| | **Marco 2 — Pedido completo pelo celular** | | | **16** | |
| 10 | Vídeos e Reels do prato | 34 | 44 | 17 | RF21 |
| 11 | Rodízio à la minute | 36 | 47 | 19 | RF35, RF36, RF37 |
| 12 | Rodízio de passagem | 14 | 18 | 19 | RF38 |
| | **Marco 3 — Cardápio com vídeos e rodízio** | | | **19** | |
| 13 | App do garçom (PWA) | 12 | 16 | 20 | — |
| 14 | Teste grátis e limites do plano | 20 | 26 | 21 | RF01, RF03 |
| 15 | Testes, segurança e desempenho | 32 | 42 | 22 | requisitos não funcionais |
| | **Marco 4 — MVP pronto** | | | **22** | |
| — | Busca pelo restaurante piloto | — | — | depende da busca | — |
| 16 | Piloto num restaurante de verdade | 48 | 62 | 24 | todos do MVP |
| | **Marco 5 — MVP validado** | | | **≈ 24 + tempo da busca** | |
| | **Total** | **544 h** | **≈ 705 h** | | |

As horas já incluem as ≈ 50 h a mais da arquitetura hexagonal em todo o sistema (modelo de domínio separado das entidades JPA, mappers e portas também nos cadastros simples, testes ArchUnit). Elas estão distribuídas principalmente nas etapas de cadastro: 1, 2, 3, 4, 6, 8, 11 e 14.

## Etapa 0 — Preparação (fora do código)

Coisas para deixar prontas antes de começar a programar.

- [ ] Escolher um **nome provisório** para o repositório e o domínio.
- [ ] Registrar o domínio (`.com.br`).
- [ ] Criar as contas: GitHub, Fly.io, Vercel, Neon, Cloudflare (R2), Bunny, Sentry e Resend.
- [ ] Verificar se há **CNPJ** (e há quanto tempo) para a cobrança da assinatura na fase 2.

## Etapa 1 — Fundação técnica

**Objetivo:** o esqueleto do sistema rodando na sua máquina e publicado num ambiente de testes (staging).

- [ ] Repositório com `backend/` e `frontend/`.
- [ ] Spring Boot 4 + Java 21: Web, Security, Validation, Data JPA, Actuator.
- [ ] Usar versões das bibliotecas compatíveis com o Spring Boot 4 (springdoc, ShedLock, Testcontainers, web-push).
- [ ] PostgreSQL + Flyway com a primeira migração.
- [ ] Next.js + TypeScript + Tailwind + shadcn/ui.
- [ ] Docker Compose com Postgres local.
- [ ] springdoc + openapi-typescript gerando os tipos do front a partir da API.
- [ ] Integração contínua (GitHub Actions): build e testes do back (com Testcontainers) e do front.
- [ ] Staging publicado: back no Fly.io, front na Vercel e banco no Neon, todos em São Paulo.
- [ ] Sentry ligado no back e no front.

**Pronto quando:** um commit na branch principal publica sozinho no staging, e a página inicial do front mostra dados vindos da API.

## Etapa 2 — Contas, login e equipe

**Objetivo:** um dono cria a conta, entra no admin e cadastra a equipe, e um restaurante nunca enxerga os dados de outro.

- [ ] Tabelas `organization`, `establishment`, `user` e `membership`.
- [ ] Cadastro self-service: nome, estabelecimento, CPF/CNPJ, WhatsApp, e-mail e senha.
- [ ] Login com e-mail e senha (JWT) e recuperação de senha por e-mail (Resend).
- [ ] Separação entre restaurantes: `establishment_id` em tudo, com filtro do Hibernate aplicado automaticamente.
- [ ] **Testes que tentam ler dados de outro restaurante e precisam falhar.**
- [ ] Cadastro da equipe com papéis (dono, gerente, garçom, cozinha).
- [ ] Vínculo de aparelho (`device`) e login rápido do garçom por PIN.
- [ ] Telas do admin: cadastro, login e equipe.

**Pronto quando:** dois restaurantes de teste convivem no staging sem ver nada um do outro, e um garçom entra por PIN num celular vinculado.

## Etapa 3 — Salão: setores, mesas e QR Code

**Objetivo:** o dono cadastra o salão e imprime o display de cada mesa.

- [ ] Tabelas `sector` e `dining_table`, com `public_token` aleatório por mesa.
- [ ] Telas de setores e mesas no admin.
- [ ] QR Code de cada mesa (ZXing) apontando para `/m/{public_token}`.
- [ ] Display para impressão em PNG, SVG e PDF (OpenPDF), com logo, nome e número da mesa.
- [ ] Botão "trocar QR desta mesa", que gera um token novo e invalida o antigo.

**Pronto quando:** você imprime o PDF, escaneia com o celular e cai na página da mesa certa (ainda vazia).

## Etapa 4 — Cardápio e fotos

**Objetivo:** o dono monta o cardápio completo com fotos.

- [ ] Tabelas `menu_category`, `product` e `product_media` (só imagens nesta etapa).
- [ ] CRUD de categorias e produtos: preço, preço promocional, destaque, disponível/indisponível e ordem.
- [ ] Envio de fotos direto para o Cloudflare R2 (URL assinada) e geração de 3 tamanhos.
- [ ] Logo e cor principal do estabelecimento.
- [ ] Telas do cardápio no admin, com arrastar para reordenar.

**Pronto quando:** um cardápio de 50 itens com fotos fica cadastrado em menos de uma hora de uso.

## Etapa 5 — Página da mesa (vitrine)

**Objetivo:** o cliente escaneia o QR e vê a tela inicial e o cardápio, como no vídeo do Chamaii.

- [ ] `/m/{token}` montada no servidor (Next), rápida no 4G.
- [ ] Criação da sessão da mesa (`table_session`) com cookie.
- [ ] Tela "Como podemos ajudar?" com logo, nome, mesa e botões (Chamar garçom, Ver cardápio, Pedir a conta); os botões ainda não funcionam.
- [ ] Cardápio com abas por categoria, Destaques, busca, preço "de/por" e itens indisponíveis em cinza.
- [ ] Links para Instagram e "Avalie no Google".

**Pronto quando:** a página abre em até 2 segundos num celular com 4G e mostra o cardápio real do restaurante de teste.

## Etapa 6 — Chamados em tempo real, painel e Modo TV

**Objetivo:** o fluxo do vídeo de ponta a ponta. A mesa chama, a equipe recebe com som, alguém aceita e a mesa vê "Garçom a caminho".

- [ ] Tabela `call` e seu ciclo: pendente → em atendimento → atendido, ou cancelado.
- [ ] Endpoint SSE por restaurante (equipe) e por sessão (mesa), com reconexão automática.
- [ ] LISTEN/NOTIFY do Postgres repassando os eventos entre instâncias do Spring.
- [ ] Botões "Chamar garçom" e "Pedir a conta" (com Pix, cartão ou dinheiro) funcionando, com opção de cancelar.
- [ ] Painel do garçom no celular: fila por ordem de espera, cronômetro e "Toque para atender".
- [ ] Modo TV: tela cheia só de leitura, letras grandes e alerta sonoro contínuo.
- [ ] Liberação do som no navegador (toque inicial) e aviso quando o som estiver bloqueado.

**Pronto quando:** do toque na mesa até o som no celular do garçom passam menos de 2 segundos, medidos no staging.

> **Marco 1 — A mesa chama o garçom (semana 12).** Dá para gravar o mesmo vídeo do Chamaii com o nosso sistema, que depois serve para apresentar o produto na busca pelo piloto.

## Etapa 7 — Trava de presença

**Objetivo:** só quem está no salão consegue chamar e pedir.

- [ ] Tabela `presence_rule` e tela no admin: IPs da internet do salão, endereço e raio no mapa.
- [ ] Ao abrir a mesa, testar o IP; se falhar, pedir a localização (GPS); se falhar, oferecer "Liberar esta mesa".
- [ ] Chamado do tipo `unlock`: o garçom libera a mesa com um toque.
- [ ] Validade da sessão (`session_ttl_minutes`) e expiração automática (`@Scheduled` + ShedLock).
- [ ] Limites de pedidos por minuto por sessão e bloqueio de sessão pela equipe.
- [ ] Painel mostra como a presença foi provada (Wi-Fi, GPS ou garçom).

**Pronto quando:** um celular fora do restaurante, mesmo com a foto do QR, não consegue chamar nem pedir sem a liberação do garçom.

## Etapa 8 — Pedidos, comanda e cozinha

**Objetivo:** o diferencial do produto, que é pedir pelo celular e o pedido chegar à cozinha.

- [ ] Tabelas `tab`, `order` e `order_item`, com preço copiado no momento do pedido.
- [ ] Carrinho na página da mesa: quantidade e observação por item.
- [ ] "Enviar pedido": abre a comanda no primeiro pedido e junta os pedidos de todos os celulares da mesa.
- [ ] Ciclo do item: na fila → em preparo → pronto → entregue, ou cancelado.
- [ ] Tela da cozinha (Modo TV) com a fila de itens; botões "em preparo" e "pronto".
- [ ] Garçom marca "entregue" e cancela item com motivo.
- [ ] Configurações: ligar/desligar pedido pelo celular, confirmação opcional do garçom, valor e quantidade máximos sem confirmação.
- [ ] Cliente cancela item só enquanto está na fila.

**Pronto quando:** três celulares na mesma mesa fazem pedidos, todos caem na mesma comanda, e a cozinha e o garçom levam os itens até "entregue".

## Etapa 9 — "A caminho", histórico e fechar a conta

**Objetivo:** o cliente acompanha tudo e pede para fechar a conta com o total certo.

- [ ] Aba "Meus pedidos" com o histórico de todos os itens da comanda (entregues e a caminho), status e horário.
- [ ] Filtro "A caminho" com o que ainda não chegou, atualizado em tempo real.
- [ ] "Pedir a conta" mostra o total da comanda e envia a forma de pagamento escolhida ao garçom.
- [ ] Garçom fecha a comanda: a mesa fica livre e as sessões expiram.
- [ ] Histórico de comandas no admin, com filtro por data e mesa.

**Pronto quando:** o total mostrado ao cliente bate com a soma dos itens não cancelados, e a comanda fechada aparece no admin.

> **Marco 2 — Pedido completo pelo celular (semana 16).** O sistema já faz mais que o Chamaii.

## Etapa 10 — Vídeos e Reels do prato

**Objetivo:** o cardápio em vídeo no estilo Stories/Reels.

- [ ] Envio de vídeo pelo admin direto para o Bunny Stream, com webhook de "conversão pronta".
- [ ] `product_media` com vários vídeos e fotos por prato, em ordem.
- [ ] Tela cheia vertical: deslizar para o próximo prato, tocar para pausar, botão de som, "1 de 3", selos (destaque, indisponível) e "leia mais".
- [ ] Botão "Adicionar ao pedido" dentro do Reels.
- [ ] Carregar o próximo vídeo antes, para não travar no 4G.

**Pronto quando:** dá para passar por 10 pratos em vídeo num celular com 4G sem tela preta entre um e outro.

## Etapa 11 — Rodízio à la minute

**Objetivo:** rodízio japonês, de pizza ou de petiscos, pedido em rodadas e sem preço.

- [ ] Tabelas `rodizio`, `rodizio_price`, `rodizio_product`, `tab_rodizio` e `tab_rodizio_guest`, e o campo `a_la_carte` em `product`.
- [ ] Admin: criar rodízio, preços por faixa (adulto, criança) e por dia/horário, produtos incluídos e regras opcionais (todas desligadas por padrão).
- [ ] Mesa: "Quero o rodízio" → garçom confirma o número de pessoas por faixa.
- [ ] Aba Rodízio sem preços, com + e − e "Enviar rodada"; os itens vão para a cozinha com preço zero.
- [ ] Regras opcionais: máximo por item por rodada, itens não entregues por pessoa, intervalo entre rodadas, tempo limite com aviso e aviso de desperdício.
- [ ] Conta: pessoas × preço do rodízio + itens à parte.

**Pronto quando:** uma mesa de 4 pessoas faz 5 rodadas, pede bebidas à parte, e a conta sai com 4 rodízios + bebidas.

## Etapa 12 — Rodízio de passagem

**Objetivo:** churrascaria e galeteria.

- [ ] Tipo "passagem" no rodízio.
- [ ] Pedido de corte específico ("picanha mal passada"), que vai para os passadores e não para a cozinha.
- [ ] Tela dos passadores (no celular) com os pedidos de corte por mesa.
- [ ] Cartão verde/vermelho digital: a mesa pausa ou volta a ser servida, e a tela dos passadores mostra o estado de cada mesa.

**Pronto quando:** um passador vê em tempo real quais mesas estão no vermelho e quais pediram um corte específico.

> **Marco 3 — Cardápio com vídeos e rodízio (semana 19).** Todo o escopo funcional do MVP está pronto.

## Etapa 13 — App do garçom (PWA)

**Objetivo:** o garçom instala o painel como um aplicativo.

- [ ] Manifest, ícones e service worker (o Next já ajuda).
- [ ] Tela de instalação com o passo a passo para Android e iPhone.
- [ ] Manter a tela acesa durante o turno (Wake Lock) e reconectar ao voltar do segundo plano.

**Pronto quando:** o painel abre pelo ícone na tela inicial, em tela cheia, no Android e no iPhone. As notificações com o celular bloqueado ficam para a fase 2.

## Etapa 14 — Teste grátis e limites do plano

**Objetivo:** o sistema já se comporta como produto, mesmo sem cobrança automática.

- [ ] Tabelas `plan` e `subscription`, com os planos aprovados (limites e entitlements).
- [ ] Teste grátis de 14 dias liberado no cadastro, com avisos por e-mail perto do fim.
- [ ] Limites aplicados: mesas, usuários, setores e vídeos (só a partir do Pro).
- [ ] Ao acabar o teste: o admin mostra "assinatura pendente" e a mesa continua funcionando por alguns dias de tolerância. A cobrança com o Asaas entra na fase 2.

**Pronto quando:** um restaurante no plano Essencial não consegue cadastrar a 21ª mesa nem enviar vídeos.

## Etapa 15 — Testes, segurança e desempenho

**Objetivo:** chegar ao piloto sem surpresas.

- [ ] Testes de ponta a ponta (Playwright) dos fluxos principais: chamar, pedir, rodízio e fechar a conta.
- [ ] Teste de carga: 30 mesas pedindo ao mesmo tempo num restaurante e 20 restaurantes simultâneos, medindo os 2 segundos do tempo real.
- [ ] Revisão de segurança: tokens das mesas, limites por IP, permissões por papel e separação entre restaurantes.
- [ ] Backups do banco conferidos (restaurar uma cópia de verdade).
- [ ] Política de privacidade e termos de uso (LGPD).
- [ ] Ambiente de produção separado do staging.

**Pronto quando:** os testes de ponta a ponta passam no CI e o teste de carga cumpre a meta de 2 segundos.

> **Marco 4 — MVP pronto (semana 22).**

## Busca pelo restaurante piloto

**Objetivo:** fechar com um restaurante para usar o sistema por pelo menos 2 semanas. Começa com o MVP pronto; a duração depende da busca.

- [ ] Gravar um vídeo de demonstração curto (estilo Reels), mostrando chamado, pedido, vídeos do cardápio e rodízio.
- [ ] Listar restaurantes da região, de preferência com rodízio, para testar tudo de uma vez.
- [ ] Definir a oferta do piloto: uso grátis durante o teste e o desconto de lançamento depois (ex.: 30% por 12 meses).
- [ ] Visitar, apresentar e combinar datas, quem participa da equipe e como será o acompanhamento.

**Pronto quando:** um restaurante topou e a data de início está marcada.

## Etapa 16 — Piloto num restaurante de verdade

**Objetivo:** usar o sistema por pelo menos 2 semanas num restaurante e corrigir o que aparecer.

- [ ] Cadastrar o restaurante junto com o dono (cardápio, fotos, vídeos e rodízio).
- [ ] Imprimir e instalar os displays nas mesas.
- [ ] Treinar garçons e cozinha (30 minutos) e instalar o PWA nos celulares.
- [ ] Acompanhar presencialmente as primeiras noites.
- [ ] Medir: chamados e pedidos por noite, tempo médio de atendimento, erros no Sentry e opinião de clientes e equipe.
- [ ] Corrigir os problemas e ajustar a experiência.

**Pronto quando:** o restaurante usa o sistema por 2 semanas seguidas sem precisar voltar ao papel, e o dono topa continuar (de preferência, pagando).

> **Marco 5 — MVP validado (≈ semana 24 + tempo da busca).** Próximo passo: a fase 2 (cobrança automática pelo Asaas, push no celular do garçom, adicionais nos pratos, filas de cozinha e bar, perfis e relatórios).

## Como reduzir o prazo, se precisar

| Corte | Economia aproximada | Impacto |
| --- | --- | --- |
| Rodízio de passagem logo depois do piloto, se o piloto não for churrascaria | ≈ 18 h | Nenhum para o piloto |
| Display só em PDF (sem PNG e SVG) | ≈ 6 h | Pequeno |
