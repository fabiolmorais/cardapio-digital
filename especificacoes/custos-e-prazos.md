# Custos de infraestrutura e prazo de desenvolvimento

21/10/2026 · Fabio Leite Morais

Comparação entre as duas stacks consideradas na [especificação](especificacao-cardapio-digital.md):

- **Stack A — Next.js + Supabase** (tudo em TypeScript; banco, login, tempo real e arquivos no Supabase).
- **Stack B — Spring Boot + Next.js** (back-end em Java no Fly.io, banco PostgreSQL no Neon, front Next.js na Vercel).

## Resumo

| Restaurantes ativos | Stack A — Next + Supabase | Stack B — Spring + Next |
| --- | --- | --- |
| 1 | ≈ R$ 230/mês | ≈ R$ 195/mês |
| 10 | ≈ R$ 300/mês | ≈ R$ 335/mês |
| 100 | ≈ R$ 1.375 a R$ 1.960/mês | ≈ R$ 1.235/mês |
| **Prazo médio do MVP** (1 dev, 30 h/semana) | **≈ 20 semanas** (≈ 615 h) | **≈ 24 semanas** (≈ 705 h), com hexagonal completo |

- **Custo:** as duas ficam na mesma faixa até 10 restaurantes. Com 100, a Stack A fica mais cara porque o Supabase cobra por mensagem de tempo real; dá para reduzir boa parte disso enviando menos eventos (um por pedido, e não um por item).
- **Prazo:** a Stack B fica cerca de 3 a 4 semanas mais longa, mas a diferença vem da escolha de usar **arquitetura hexagonal em todo o sistema** (≈ 50 h a mais), não da linguagem. Sem ela, as duas ficariam praticamente iguais: a Stack A já vem com login e tempo real prontos, mas exigiria aprender o Supabase e escrever o back-end em TypeScript; a Stack B exige montar login e tempo real, mas em Java, que você já domina.
- Em nenhum cenário a infraestrutura é o gasto que decide o negócio: com 100 restaurantes pagando em média R$ 100, a receita é R$ 10 mil/mês, e a infraestrutura fica entre 12% e 20% disso.

Câmbio usado: **US$ 1 = R$ 5,03** (fechamento de 21/10/2026, [SpaceMoney](https://www.spacemoney.com.br/economia/dolar-hoje/dolar-alta-pesquisa-quaest-segundo-turno)). Valores em dólar convertidos e arredondados; impostos sobre serviços no exterior (IOF do cartão) não estão incluídos.

## Premissas de uso

Os custos dependem de quanto cada restaurante usa o sistema. Usei um restaurante médio:

| Premissa | Valor por restaurante |
| --- | --- |
| Mesas | 30 |
| Dias de funcionamento | 25 por mês |
| Atendimentos de mesa (sessões) | 120 por dia ≈ 3.000 por mês |
| Sessões em que o cliente assiste aos Reels | 40%, cerca de 5 vídeos de ~2 MB cada |
| Tráfego de vídeo | ≈ 12 GB por mês |
| Fotos do cardápio | ~100 fotos em 3 tamanhos ≈ 45 MB |
| Vídeos guardados | ~40 vídeos ≈ 360 MB |
| Aparelhos da equipe conectados no pico | ~8 (celulares, TV, cozinha) |
| Clientes conectados no pico | ~15 (só enquanto há chamado ou pedido aberto) |
| Eventos de tempo real por sessão | ~25, entregues a ~9 aparelhos ≈ 225 mensagens |
| Pico simultâneo entre restaurantes | 70% dos restaurantes ao mesmo tempo |

Se o uso real for o dobro, os itens que crescem com o uso (vídeo, mensagens de tempo real, processamento) dobram junto; os planos fixos continuam iguais.

## Stack A — Next.js + Supabase

| Serviço | 1 restaurante | 10 restaurantes | 100 restaurantes | Base de cálculo |
| --- | --- | --- | --- | --- |
| [Vercel Pro](https://vercel.com/pricing) (app e API) | US$ 20 | US$ 20 | ≈ US$ 35 | US$ 20 por desenvolvedor, com US$ 20 de crédito de uso e 1 TB de tráfego; com 100 restaurantes, cerca de 12 milhões de chamadas à API por mês passam do crédito |
| [Supabase Pro](https://supabase.com/pricing) — plano e servidor do banco | US$ 25 | US$ 30 | US$ 75 | US$ 25 com servidor Micro incluso; Small (+US$ 5 líquido) com 10; Medium (+US$ 50 líquido) com 100 |
| Supabase — conexões de tempo real | incluso | incluso | ≈ US$ 20 | 500 conexões simultâneas inclusas; com 100 restaurantes o pico é ~1.600; US$ 10 por 1.000 a mais |
| Supabase — mensagens de tempo real | incluso | ≈ US$ 4 | ≈ US$ 156 (≈ US$ 40 otimizado) | 5 milhões/mês inclusos; US$ 2,50 por milhão a mais; 100 restaurantes geram ~67 milhões |
| [Bunny Stream](https://bunny.net/pricing/stream/) (vídeos) | US$ 1 (mínimo) | ≈ US$ 5,50 | ≈ US$ 54,50 | Entrega na América do Sul a [US$ 0,045/GB](https://bunny.net/pricing/cdn/); armazenamento a US$ 0,01/GB; conversão grátis |
| [Cloudflare R2](https://developers.cloudflare.com/r2/pricing/) (fotos) | US$ 0 | US$ 0 | US$ 0 | 10 GB grátis e saída de dados sem custo; 100 restaurantes usam ~4,5 GB |
| [Resend](https://resend.com/pricing) (e-mails) | US$ 0 | US$ 0 | US$ 20 | 3.000 e-mails/mês grátis; plano de US$ 20 quando passar disso |
| [Sentry](https://sentry.io/pricing/) (erros) | US$ 0 | US$ 0 | US$ 29 | Grátis para 1 pessoa e 5.000 erros; Team quando crescer |
| **Total em dólar** | **≈ US$ 46** | **≈ US$ 60** | **≈ US$ 390 (≈ US$ 275 otimizado)** | |
| **Total em reais** | **≈ R$ 230** | **≈ R$ 300** | **≈ R$ 1.960 (≈ R$ 1.375 otimizado)** | |

O Vercel Hobby (grátis) não pode ser usado para fins comerciais, então o plano Pro é obrigatório desde o primeiro cliente.

## Stack B — Spring Boot + Next.js

| Serviço | 1 restaurante | 10 restaurantes | 100 restaurantes | Base de cálculo |
| --- | --- | --- | --- | --- |
| [Vercel Pro](https://vercel.com/pricing) (só o front) | US$ 20 | US$ 20 | ≈ US$ 25 | Mesmo plano; o front faz bem menos chamadas, porque a API fica no Spring |
| [Fly.io](https://docs.fly.io/about/pricing) (back-end Spring, região São Paulo) | ≈ US$ 8 | ≈ US$ 16 | ≈ US$ 36 | 1 máquina de 1 GB; depois 2 máquinas de 1 GB; com 100, 2 máquinas de 2 GB + tráfego a US$ 0,04/GB. O preço exato da região São Paulo não é publicado; estimei ~20% acima do preço base (US$ 6,70 para 1 GB e US$ 13,39 para 2 GB) |
| [Neon](https://neon.com/pricing) (PostgreSQL, [região São Paulo](https://neon.com/docs/introduction/regions)) | ≈ US$ 10 | ≈ US$ 25 | ≈ US$ 81 | US$ 0,106 por hora de processamento e US$ 0,35/GB; o banco desliga sozinho quando o restaurante está fechado; com 100, fica ligado o tempo todo (1 unidade ≈ US$ 77) + ~12 GB |
| Tempo real (SSE no próprio Spring) | US$ 0 | US$ 0 | US$ 0 | Sem cobrança por conexão ou mensagem; o custo já está nas máquinas do Fly.io |
| [Bunny Stream](https://bunny.net/pricing/stream/) (vídeos) | US$ 1 | ≈ US$ 5,50 | ≈ US$ 54,50 | Igual à Stack A |
| [Cloudflare R2](https://developers.cloudflare.com/r2/pricing/) (fotos) | US$ 0 | US$ 0 | US$ 0 | Igual à Stack A |
| [Resend](https://resend.com/pricing) e [Sentry](https://sentry.io/pricing/) | US$ 0 | US$ 0 | US$ 49 | Iguais à Stack A |
| **Total em dólar** | **≈ US$ 39** | **≈ US$ 67** | **≈ US$ 245** | |
| **Total em reais** | **≈ R$ 195** | **≈ R$ 335** | **≈ R$ 1.235** | |

Alternativa de banco para a Stack B: o [Fly.io Managed Postgres](https://fly.io/docs/mpg) Basic custa US$ 38/mês (com réplica e backup) mais US$ 0,28/GB, também em São Paulo. Fica mais caro que o Neon com poucos clientes e parecido com 100.

## Custos que não dependem da stack

| Item | 1 restaurante | 10 restaurantes | 100 restaurantes |
| --- | --- | --- | --- |
| Taxa do gateway de assinatura (Asaas, cartão: R$ 0,49 + 2,99% de uma mensalidade de R$ 69,90) | ≈ R$ 2,60 | ≈ R$ 26 | ≈ R$ 258 |
| Domínio `.com.br` | ≈ R$ 40 por ano | | |

## Custo por cliente e peso na receita

Considerando uma mensalidade média de R$ 100 (mistura de Essencial e Pro):

| Restaurantes | Receita/mês | Stack A: custo por cliente (% da receita) | Stack B: custo por cliente (% da receita) |
| --- | --- | --- | --- |
| 1 | R$ 100 | R$ 230 (230%, prejuízo) | R$ 195 (195%, prejuízo) |
| 10 | R$ 1.000 | R$ 30 (30%) | R$ 34 (34%) |
| 100 | R$ 10.000 | R$ 14 a R$ 20 (14% a 20%) | R$ 12 (12%) |

Com um só cliente a conta não fecha em nenhuma das stacks; o sistema passa a se pagar entre 3 e 4 clientes.

## Prazo de desenvolvimento do MVP

Estimativa para **você sozinho**, com o escopo atual do MVP (chamados, cardápio com fotos e vídeos, pedido pelo celular, comanda, histórico, trava de presença e rodízio dos dois tipos). A cobrança da assinatura fica fora, porque é da fase 2; o MVP tem cadastro e teste grátis.

| Módulo | Stack A (horas) | Stack B (horas) | Por que muda |
| --- | --- | --- | --- |
| Projeto, deploy automático e ambientes | 16 | 32 | Na B são duas aplicações para publicar |
| Login, separação entre restaurantes, equipe e PIN | 32 | 48 | Na A o login vem pronto; na B você monta com Spring Security |
| Setores, mesas, QR Code e display para impressão | 24 | 24 | |
| Cardápio, categorias e fotos | 32 | 36 | |
| Vídeos: envio para o Bunny e tela de Reels | 32 | 34 | |
| Página da mesa | 32 | 32 | Mesmo front nas duas |
| Trava de presença (Wi-Fi, GPS, liberação pelo garçom) | 24 | 24 | |
| Chamados em tempo real, painel do garçom, Modo TV e alerta sonoro | 32 | 48 | Na B você monta o SSE e a distribuição entre servidores |
| Pedidos: carrinho, comanda, status dos itens e tela da cozinha | 56 | 52 | Regras de negócio rendem mais em Java para você |
| Histórico e "A caminho" | 12 | 12 | |
| Rodízio (à la minute e passagem, regras opcionais) | 48 | 44 | Idem |
| App do garçom (PWA) | 12 | 12 | |
| Cadastro, teste grátis e limites do plano | 16 | 16 | |
| Testes automatizados | 32 | 32 | |
| Piloto: ajustes, correções e melhorias de uso | 48 | 48 | |
| Aprender Supabase, regras de segurança no banco e back-end em TypeScript | 24 | 0 | Você já domina Java/Spring |
| Arquitetura hexagonal em todo o sistema (modelo de domínio separado das entidades JPA, mappers e portas também nos cadastros simples, testes ArchUnit) | — | 50 | Decisão de 21/10/2026; na Stack B o roadmap distribui essas horas pelas etapas |
| **Total (cenário otimista)** | **472 h** | **544 h** | |

Projetos solo quase sempre passam da estimativa inicial. Por isso, calculei três cenários e a média entre eles:

| Cenário | Stack A | Stack B |
| --- | --- | --- |
| Otimista (tudo dá certo) | 472 h | 544 h |
| Realista (+30%) | 614 h | 707 h |
| Pessimista (+60%) | 755 h | 870 h |
| **Média** | **≈ 615 h** | **≈ 705 h** |

Convertendo a média em calendário:

| Dedicação | Stack A | Stack B |
| --- | --- | --- |
| Tempo integral (30 h produtivas por semana) | ≈ 20 semanas (≈ 5 meses) | ≈ 24 semanas (≈ 5,5 meses) |
| Meio período (15 h por semana) | ≈ 41 semanas (≈ 9,5 meses) | ≈ 47 semanas (≈ 11 meses) |

Sem a arquitetura hexagonal completa, a diferença entre as stacks seria de ≈ 1 semana, dentro da margem de erro; com ela, a Stack B leva ≈ 3 a 4 semanas a mais, em troca do aprendizado e de um código mais fácil de manter e testar. O que mais encurta o prazo é reduzir o escopo do primeiro piloto: por exemplo, lançar com o rodízio à la minute e deixar o de passagem para logo depois economiza cerca de 15 a 20 horas.

## Fontes

Preços consultados em 21/10/2026:

- [Supabase — preços](https://supabase.com/pricing) e [regiões](https://supabase.com/docs/guides/platform/regions)
- [Vercel — preços](https://vercel.com/pricing)
- [Fly.io — preços](https://docs.fly.io/about/pricing) e [Managed Postgres](https://fly.io/docs/mpg)
- [Neon — preços](https://neon.com/pricing) e [regiões](https://neon.com/docs/introduction/regions)
- [Bunny Stream](https://bunny.net/pricing/stream/) e [Bunny CDN](https://bunny.net/pricing/cdn/)
- [Cloudflare R2](https://developers.cloudflare.com/r2/pricing/)
- [Resend](https://resend.com/pricing)
- [Sentry](https://sentry.io/pricing/)
- [Asaas — taxas](https://www.asaas.com/precos-e-taxas)
- [Cotação do dólar — SpaceMoney](https://www.spacemoney.com.br/economia/dolar-hoje/dolar-alta-pesquisa-quaest-segundo-turno)
