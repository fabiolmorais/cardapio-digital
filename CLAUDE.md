# CLAUDE.md — Regras de desenvolvimento

Regras que todo código deste repositório segue, escrito por pessoas ou por assistentes de IA. Em caso de dúvida, a especificação manda; se uma regra daqui precisar mudar, registre a decisão num ADR (ver "Decisões") e atualize este arquivo.

## Sobre o projeto

SaaS de cardápio digital e atendimento por mesa para bares e restaurantes: o cliente escaneia o QR Code, chama o garçom, vê o cardápio com fotos e vídeos, faz pedidos pelo celular, acompanha os itens e pede para fechar a conta; a equipe recebe tudo em tempo real.

- Especificação do sistema: `especificacoes/especificacao-cardapio-digital.md`
- Roadmap do MVP: `especificacoes/roadmap-mvp.md`
- Custos e prazos: `especificacoes/custos-e-prazos.md`
- Referência do concorrente (não é requisito): `especificacoes/referencia-chamaii.md`

## Stack

- **Back-end:** Java 21, Spring Boot 4, Spring Security (JWT), Spring Data JPA, Bean Validation, Flyway, SSE.
- **Banco:** PostgreSQL (Neon em produção; Docker local).
- **Front-end:** Next.js (App Router), TypeScript, Tailwind, shadcn/ui, TanStack Query.
- **Integrações:** Cloudflare R2 (fotos), Bunny Stream (vídeos), Resend (e-mail), web-push (PWA), Asaas (fase 2).
- **Hospedagem:** Fly.io (back), Vercel (front), Neon (banco), todos em São Paulo.
- **Testes:** JUnit 5, Testcontainers, ArchUnit, Playwright.

## Comandos

```bash
docker compose up -d                 # Postgres local
cd backend && ./mvnw spring-boot:run # back-end
cd backend && ./mvnw verify          # testes do back (unitários, integração, ArchUnit)
cd frontend && npm run dev           # front-end
cd frontend && npm run gen:api       # gera os tipos a partir do OpenAPI do back
cd frontend && npm run test:e2e      # Playwright
```

## Idioma

- **Código, banco e API em inglês:** classes, métodos, variáveis, tabelas, colunas, endpoints (`Tab`, `Order`, `Call`, `/api/v1/tables`).
- **Português** na interface (pt-BR), na documentação, nos ADRs, nos comentários e nas mensagens de commit.
- Os nomes do domínio seguem a modelagem da especificação (`establishment`, `dining_table`, `tab`, `order_item`, `rodizio`…). "Rodizio" fica como está, por ser um termo do negócio sem tradução boa.

## Arquitetura: monolito hexagonal

Uma única aplicação Spring Boot (sem divisão em módulos ou microsserviços), organizada em **arquitetura hexagonal (portas e adaptadores)** em todo o sistema.

### Estrutura de pacotes do back-end

```
br.com.<produto>/
├── domain/                     # núcleo: regras de negócio puras
│   └── <feature>/              # ex.: order, tab, rodizio, call, menu, presence
│       ├── model/              # entidades, value objects (records), enums
│       ├── service/            # serviços de domínio (regras que envolvem várias entidades)
│       ├── event/              # eventos de domínio
│       └── exception/          # exceções de negócio
├── application/                # casos de uso
│   ├── port/in/<feature>/      # interfaces dos casos de uso + comandos e resultados (records)
│   ├── port/out/<feature>/     # interfaces que o núcleo precisa (repositórios, mídia, e-mail, eventos)
│   └── service/<feature>/      # implementações dos casos de uso
├── adapter/
│   ├── in/web/<feature>/       # controllers REST, DTOs, mappers DTO ↔ comando
│   ├── in/sse/                 # endpoints de tempo real
│   ├── in/scheduler/           # tarefas agendadas
│   ├── out/persistence/<feature>/ # entidades JPA, repositórios Spring Data, adaptadores, mappers
│   ├── out/realtime/           # publicação de eventos (LISTEN/NOTIFY + SSE)
│   ├── out/storage/            # Cloudflare R2
│   ├── out/video/              # Bunny Stream
│   ├── out/email/              # Resend
│   ├── out/push/               # web-push
│   └── out/payment/            # Asaas (fase 2)
└── config/                     # criação dos beans, segurança, filtro de restaurante, propriedades
```

### Regras de dependência (verificadas por ArchUnit)

1. `domain` **não depende de nada** do projeto nem de framework: sem Spring, sem JPA (`jakarta.persistence`), sem Jackson, sem Lombok. Só Java.
2. `application` depende só de `domain`. A única anotação de framework permitida é `@Transactional`, e só nos serviços de caso de uso.
3. `adapter` depende de `application` (portas) e de `domain`. Um adaptador nunca chama outro adaptador diretamente.
4. Controllers chamam **portas de entrada** (`port/in`), nunca serviços concretos nem repositórios.
5. Os serviços de caso de uso não usam `@Service`/`@Component`: são registrados como beans em classes de `config/`.
6. Entidades JPA ficam só em `adapter/out/persistence` e nunca saem dele; a conversão para o modelo de domínio é feita por mappers.
7. Um teste ArchUnit para cada regra acima roda no `./mvnw verify`. Build quebrado por ArchUnit não se contorna: corrige-se o código.

### Domínio

- Regras de negócio moram em `domain` (ex.: "o cliente só cancela item em `PENDING`", "o rodízio cobra por pessoa", "a comanda abre no primeiro pedido").
- Entidades protegem seus invariantes: sem setters públicos; mudança de estado por métodos com nome do negócio (`item.markAsReady()`, `tab.close(...)`).
- Value objects como `record` imutáveis (`Money`, `EstablishmentId`, `TableToken`).
- Dinheiro sempre em `Money` (centavos em `long` ou `BigDecimal`), nunca `double`/`float`.
- Datas e horas em `Instant` (UTC) no domínio e no banco; conversão para o fuso do restaurante só na apresentação.
- Comunicação entre partes do domínio por eventos de domínio quando um efeito não é obrigatório na mesma transação (ex.: `OrderItemReady` → tempo real).

## Separação entre restaurantes (multi-tenant) — regra crítica

Vazamento de dados entre restaurantes é o pior erro possível neste sistema.

- Toda tabela de dados de restaurante tem `establishment_id` com índice.
- O `establishmentId` vem **sempre** do usuário autenticado (ou do token da mesa), resolvido no adaptador web, e é passado explicitamente em todo comando de caso de uso. Nunca é aceito de parâmetro, corpo ou cabeçalho enviados pelo cliente.
- Toda porta de repositório recebe o `establishmentId`, e o adaptador de persistência também ativa o filtro do Hibernate por restaurante (defesa em dobro).
- **Todo endpoint novo que lê ou altera dados tem um teste de integração provando que um usuário de outro restaurante recebe 404.**
- IDs expostos são UUID ou tokens aleatórios, nunca sequenciais.

## Segurança

- Senhas e PINs com hash (Argon2id ou BCrypt). PIN só vale em aparelho já vinculado ao restaurante.
- JWT de vida curta, com renovação controlada e revogação (ex.: garçom desligado).
- Autorização no back-end em todo endpoint (`@PreAuthorize` por papel: `OWNER`, `MANAGER`, `STAFF`, `KITCHEN`). O front esconder um botão não é segurança.
- Limite de tentativas (rate limit) no login, no PIN e nos endpoints da mesa (chamados, pedidos).
- Validação de toda entrada com Bean Validation nos DTOs; regras de negócio validadas de novo no domínio.
- A API nunca devolve entidades de domínio nem JPA: só DTOs.
- Erros no padrão Problem Details (RFC 9457), sem stack trace, SQL ou detalhes internos.
- Só consultas parametrizadas (JPA/Spring Data). Proibido montar SQL concatenando texto.
- Uploads por URL assinada com validade curta direto para R2/Bunny; o back confere tipo real e tamanho.
- HTTPS em tudo; CORS só para o domínio do front; cabeçalhos CSP, HSTS e X-Frame-Options; cookies `HttpOnly`, `Secure`, `SameSite`.
- O usuário do banco usado pela aplicação não cria nem apaga tabelas; só o Flyway altera a estrutura.
- Trava de presença da mesa conforme a especificação (Wi-Fi/IP, GPS, liberação pelo garçom, sessão com validade).
- OWASP Top 10 como checklist de revisão.

### Segredos

- Nenhuma senha, chave ou token no código, nos testes ou no Git. Configuração por variáveis de ambiente e cofres de segredos do Fly.io e da Vercel.
- `.env.example` com os nomes das variáveis, sem valores reais. `.env` está no `.gitignore`.
- Gitleaks no CI para barrar segredos commitados.

### Dependências

- Dependabot ligado; OWASP Dependency-Check (back) e `npm audit` (front) no CI.
- Dependência nova só com justificativa no pull request.

## LGPD e dados pessoais

- Guardar só o necessário. O cliente da mesa é anônimo; IP e identificador do aparelho têm prazo de retenção definido e são apagados depois.
- **Logs nunca contêm** e-mail, CPF/CNPJ, telefone, senha, PIN, tokens ou corpo de requisição com dados pessoais.
- Deve existir forma de exportar e apagar os dados de um restaurante a pedido.

## API REST

- Prefixo `/api/v1`; recursos no plural e em inglês (`/api/v1/tables/{id}/calls`).
- Verbos HTTP corretos; códigos de status corretos (201 na criação, 404 também para recurso de outro restaurante).
- Listas sempre paginadas.
- A descrição OpenAPI (springdoc) é a fonte da verdade: o front gera os tipos a partir dela (`npm run gen:api`) e não declara tipos da API à mão.

## Banco de dados

- Toda mudança de estrutura é uma migração Flyway nova (`V<número>__<descricao>.sql`). **Nunca editar migração já aplicada.**
- Nomes em inglês, `snake_case`, tabelas no singular (como na especificação).
- Índice em toda chave estrangeira e em `establishment_id`.
- Dados de negócio não são apagados fisicamente: comandas, pedidos e chamados são permanentes; cadastros usam desativação (`active = false`).
- Sem consultas N+1: usar `fetch join`/projeções; os testes de integração falham se o Hibernate detectar N+1.

## Tempo real

- Eventos do servidor para as telas via SSE, um canal por restaurante (equipe) e um por sessão de mesa (cliente).
- Entre instâncias, eventos passam pelo `LISTEN/NOTIFY` do PostgreSQL.
- Payload do evento mínimo (tipo + IDs + status); a tela busca o detalhe pela API se precisar. Nunca enviar dados de um restaurante no canal de outro.
- Meta: menos de 2 segundos da ação até a tela.

## Front-end

- Organização por feature (`app/(mesa)`, `app/(painel)`, `app/(admin)`), componentes do shadcn/ui, sem CSS solto.
- A página da mesa é renderizada no servidor e tem orçamento de **menos de 200 KB de JavaScript**; abrir em até 2 s no 4G.
- Regras de negócio ficam no back-end; o front só apresenta e chama a API.
- Acessibilidade: contraste adequado, alvos de toque grandes, textos alternativos e navegação por leitor de tela.
- Todos os textos em pt-BR, centralizados (sem texto solto espalhado nos componentes), valores em R$.

## Testes

- **Domínio:** teste unitário para toda regra de negócio, sem Spring. Meta de cobertura de 80% em `domain` e `application`.
- **Adaptadores:** testes de integração com Testcontainers (PostgreSQL de verdade), sem H2.
- **Arquitetura:** ArchUnit com as regras de dependência.
- **Multi-tenant:** teste de acesso cruzado em todo endpoint de dados.
- **Ponta a ponta:** Playwright nos fluxos principais: chamar garçom, fazer pedido, rodízio, fechar conta.
- Bug corrigido ganha um teste que o reproduz.

## Observabilidade

- Logs estruturados em JSON com `requestId` e `establishmentId` (nunca dados pessoais).
- Sentry no back e no front; Actuator com health check para o Fly.io.
- Métrica do tempo entre o chamado/pedido e o evento entregue.

## Git e entrega

- `main` sempre publicável. Trabalho em branches curtas (`feat/...`, `fix/...`) e merge por pull request, mesmo trabalhando sozinho, para o CI rodar antes.
- Commits no padrão Conventional Commits, com descrição em português (`feat: envia rodada do rodízio para a cozinha`).
- CI: build, testes, ArchUnit, Gitleaks e verificação de dependências; nada vai para a `main` com CI vermelho.
- Merge na `main` publica no staging; produção só depois de validar no staging.

## Ambientes

- **Local:** Docker Compose. **Staging** e **produção:** bancos e segredos separados.
- Dados reais de restaurantes nunca são copiados para local ou staging; usar dados fictícios (seed).

## Decisões (ADRs)

- Decisões de arquitetura ficam em `docs/adr/NNNN-titulo.md` (contexto, decisão, consequências), em português.
- Já tomadas: monolito hexagonal; Spring Boot 4 + Next.js + PostgreSQL; SSE + LISTEN/NOTIFY para tempo real; código em inglês e documentação em português.

## Definição de pronto

Uma tarefa só está pronta quando:

- [ ] cumpre o requisito da especificação (RFxx) e os critérios da etapa do roadmap;
- [ ] tem testes (unitários no domínio, integração nos adaptadores) e todos passam, inclusive ArchUnit;
- [ ] endpoints novos têm teste de acesso entre restaurantes;
- [ ] mudanças no banco estão numa migração Flyway nova;
- [ ] os tipos do front foram regenerados, se a API mudou;
- [ ] não há segredos nem dados pessoais em código ou logs;
- [ ] funciona no staging.

## Instruções para o Claude

- Siga as regras deste arquivo; se uma regra atrapalhar uma tarefa, pergunte antes de abrir exceção.
- Respeite a arquitetura hexagonal: comece pelo domínio e pelas portas, depois os adaptadores.
- Antes de dizer que terminou, rode os testes do que foi alterado (`./mvnw verify` e/ou os testes do front) e informe o resultado.
- Não adicione dependências, não altere migrações antigas e não mexa em configurações de segurança sem avisar.
- Se uma decisão mudar a especificação, atualize o arquivo correspondente em `especificacoes/`.
- Responda e documente em português; escreva o código em inglês.
