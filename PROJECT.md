    # MiniVale — Carteira de Benefícios e Pagamentos

Projeto de estudo para a vaga **Desenvolvedor Backend Java Pleno — Valeshop**.

O objetivo é construir, de forma incremental, uma carteira digital de benefícios (estilo VR/VA/VT) que exercite na prática todos os tópicos do banco de perguntas técnicas: Java 17+, Spring Boot, APIs REST, JWT/OAuth2, OTP/2FA, Oracle/SQL avançado, Redis, mensageria, microsserviços, Docker, CI/CD e o domínio fintech/adquirência.

A cada fase você estuda o conceito e implementa na hora, seguindo o formato "Agir" do CBL: conceito → quando usar → quando NÃO usar → exemplo prático.

---

## Visão geral do domínio

Usuários possuem contas com saldo. Eles consultam saldo, transferem valores entre contas, consultam extrato e recebem comprovantes. Cada operação financeira precisa ser atômica, idempotente e auditável. O saldo nunca pode ficar negativo nem ser debitado duas vezes.

Esse domínio é proposital: força BigDecimal, concorrência, idempotência e consistência de saldo, que são exatamente os pontos cobrados nos capítulos de fintech e alta volumetria.

---

## Stack alvo (ao final de todas as fases)

- Java 17+ (records, sealed classes, pattern matching, Stream API, Optional)
- Spring Boot 3 (Web, Data JPA, Security, Validation)
- Banco relacional: Oracle (ou Postgres local no início, migrando para Oracle depois)
- Redis (cache, rate limiting, lock distribuído, blacklist de token)
- Kafka ou RabbitMQ (eventos de pagamento)
- Docker e docker-compose
- GitLab CI/CD
- JUnit 5 + Mockito + Testcontainers
- Resilience4j (circuit breaker, retry, timeout)

---

## Fase 1 — Monólito: núcleo da carteira ✅ CONCLUÍDA

**Objetivo:** ter uma API funcional de conta, saldo e transferência, com persistência e transações corretas.

**O que construir:**
- Entidades: `Conta`, `Transacao`, `Usuario`
- Endpoints: criar conta, consultar saldo, transferir, listar extrato
- Persistência com Spring Data JPA
- Transferência atômica com `@Transactional`
- Controle de concorrência com locking otimista (`@Version`)
- Tratamento global de erros com `@ControllerAdvice` e contrato de erro padronizado (problem+json / RFC 7807)
- Validação de entrada com Bean Validation (`@Valid`, `@NotNull`, `@Positive`)

**Conceitos exercitados (capítulos 1, 2, 3, 4, 11):**
- BigDecimal para valores monetários (nunca double/float)
- Records para DTOs, Optional, Stream API
- IoC/DI, escopos de bean, injeção por construtor
- @Transactional, propagação, isolamento, armadilha de self-invocation
- Verbos HTTP, idempotência, status codes corretos (200, 201, 204, 400, 409, 422)
- POO, SOLID, Strategy para regras de cálculo de benefício (VR, VA, VT)
- Saldo como append-only ledger (registro imutável de movimentações)

**Critério de pronto:**
- [x] Transferência entre duas contas debita uma e credita outra de forma atômica
- [x] Saldo nunca fica negativo (validação de domínio)
- [x] Tentativa de transferência inválida retorna status code e corpo de erro corretos
- [x] Cobertura de testes unitários do serviço de transferência (JUnit 5 + Mockito)

---

## Fase 2 — Segurança: JWT, refresh token e 2FA

**Objetivo:** proteger a API com autenticação stateless e confirmar transferências com OTP.

**O que construir:**
- Login com emissão de access token (JWT) e refresh token
- Renovação de token via refresh token
- Logout / revogação de token (blacklist no Redis ou tokens de vida curta)
- Confirmação de transferência via OTP (TOTP ou OTP por canal simulado)
- Autorização baseada em papéis/escopos (`@PreAuthorize`, roles vs authorities)
- Armazenamento de senha com BCrypt (salt, por que nunca MD5/SHA1 puros)

**Conceitos exercitados (capítulo 5):**
- Autenticação vs autorização
- Estrutura do JWT (header, payload, signature) e por que não colocar dado sensível no payload
- Stateful (sessão) vs stateless (token)
- Access token vs refresh token e por que o refresh existe
- Assinatura simétrica (HS256) vs assimétrica (RS256)
- OTP, 2FA, diferença entre TOTP, HOTP e OTP por SMS, e o risco de SIM swap
- Prevenção de SQL injection e itens do OWASP Top 10
- Proteção contra replay attack no endpoint de transferência

**Critério de pronto:**
- Endpoints protegidos exigem token válido
- Refresh token renova o access token
- Transferência só é confirmada após validação do OTP
- Onde fica o estado intermediário do login com 2FA está documentado

---

## Fase 3 — Performance: Redis, cache e SQL avançado

**Objetivo:** acelerar leituras, proteger endpoints críticos e dominar SQL avançado no extrato.

**O que construir:**
- Cache de consulta de saldo com Redis (estratégia cache-aside) e TTL bem definido
- Rate limiting por cliente (token bucket com Redis)
- Idempotency key em requisições de transferência (evitar débito duplicado por timeout de rede)
- Extrato com saldo acumulado por cliente usando window function (SQL)
- Paginação eficiente para listar milhões de transações
- Índices e leitura de plano de execução (EXPLAIN PLAN)

**Conceitos exercitados (capítulos 6, 7, 4):**
- Por que o Redis é rápido (in-memory), estruturas de dados e TTL
- Estratégias cache-aside, write-through, write-behind
- Cache stampede, penetration, avalanche e como mitigar
- Política de invalidação de cache
- Eviction policies (LRU, LFU) e durabilidade (RDB vs AOF)
- Window functions (ROW_NUMBER, RANK, LAG/LEAD), CTEs, subqueries
- Índices B-Tree vs bitmap, custo de escrita, função em coluna no WHERE quebrando índice
- ACID, níveis de isolamento e fenômenos de leitura
- Idempotência em API de pagamento

**Critério de pronto:**
- Consulta de saldo serve do cache sem entregar dado financeiro desatualizado
- Requisição de pagamento repetida com a mesma idempotency key não gera débito duplicado
- Extrato retorna saldo acumulado correto via window function
- Endpoint crítico respeita o rate limit por cliente

---

## Fase 4 — Distribuído: mensageria e microsserviços

**Objetivo:** quebrar o monólito em serviços e processar eventos de pagamento de forma assíncrona, garantindo consistência.

**O que construir:**
- Separar serviços: Carteira, Notificação e Antifraude
- Ao confirmar um pagamento: publicar evento que notifica o lojista, atualiza saldo e gera comprovante
- Garantia de idempotência no consumidor (entrega at-least-once)
- Dead Letter Queue para mensagens com falha e tratamento de poison message
- Retry com backoff sem loop infinito
- Transação distribuída com padrão Saga (orquestração vs coreografia) e compensação
- Transactional Outbox para consistência entre escrever no banco e publicar evento
- Resiliência com Resilience4j: circuit breaker, retry, timeout, bulkhead, fallback
- Observabilidade: logs estruturados e correlation/trace id

**Conceitos exercitados (capítulos 8, 9, 11):**
- Síncrono vs assíncrono, producer/consumer/topic/broker
- Kafka (partições, offsets, consumer groups, chave de partição) vs RabbitMQ (exchanges, bindings, routing keys)
- Garantias de entrega: at-most-once, at-least-once, exactly-once
- Consumer lag, ack/commit de offset
- Boundaries de microsserviço e bounded context (DDD)
- Clean Architecture / arquitetura hexagonal e a regra de dependência
- Entidade, value object, aggregate, repository, domain service
- Database per service, teorema CAP, consistência forte vs eventual
- Saga + compensação quando um serviço falha no meio

**Critério de pronto:**
- Confirmação de pagamento dispara o fluxo assíncrono completo
- Mensagem reentregue não debita o saldo duas vezes
- Falha em um dos serviços da Saga aciona compensação e mantém consistência
- Poison message vai para a DLQ sem travar a fila

---

## Fase 5 — Infra: Docker, docker-compose e CI/CD

**Objetivo:** empacotar tudo em containers e ter uma pipeline que builda, testa, escaneia e faz deploy com gates de qualidade.

**O que construir:**
- Dockerfile multi-stage para reduzir o tamanho da imagem Java
- Imagem base slim/distroless, usuário não-root, .dockerignore
- docker-compose subindo app + banco + Redis + Kafka/RabbitMQ
- Pipeline `.gitlab-ci.yml` com estágios: build, test, lint, security scan, deploy
- Deploy só após aprovação (gate manual)
- Gerência de segredos sem expor no código
- Cobertura mínima de testes e bloqueio de vulnerabilidades conhecidas
- Conventional commits e versionamento semântico

**Conceitos exercitados (capítulo 10):**
- Imagem vs container, container vs máquina virtual
- Dockerfile: RUN, CMD, ENTRYPOINT
- Multi-stage build, volumes vs bind mounts
- Merge vs rebase, GitFlow vs trunk-based
- Estágios de uma pipeline CI/CD, stages/jobs/runners no GitLab
- Remediação de senha commitada por engano

**Critério de pronto:**
- `docker-compose up` sobe o ambiente completo
- A pipeline falha se um teste quebra ou a cobertura cai abaixo do mínimo
- Deploy exige aprovação manual
- Nenhum segredo está hardcoded no repositório

---

## Fase 6 (opcional) — Cenários de alta volumetria

**Objetivo:** simular picos e fechamento de lote, fechando os pontos de fintech e alta volumetria.

**O que construir:**
- Job de fechamento diário processando milhões de registros em janela limitada, paralelizado com segurança e idempotência
- Tratamento de estorno (refund) sem gerar saldo negativo nem duplicidade
- Reconciliação automática de transações presas em estado intermediário após queda de serviço
- Conciliação entre sistemas que podem divergir
- Simulação de pico tipo Black Friday (volume x10) absorvendo sem perder transações
- Auditoria e rastreabilidade das operações sensíveis
- Notas sobre LGPD/PCI-DSS no tratamento de dados de pagamento

**Conceitos exercitados (capítulo 11):**
- Ledger append-only e imutabilidade de registros financeiros
- Particionamento, sharding, processamento assíncrono, backpressure
- Consistência de saldo sob alta concorrência
- Fluxo de adquirência em alto nível: autorização, captura, liquidação

**Critério de pronto:**
- Estorno nunca gera saldo negativo nem duplicidade
- Job de fechamento é idempotente e pode ser reexecutado com segurança
- Transação presa é reconciliada automaticamente

---

## Como estudar com este roteiro

1. Antes de cada fase, leia o capítulo correspondente do banco de perguntas.
2. Implemente a funcionalidade da fase tentando responder, no código, "quando usar" e "quando NÃO usar".
3. Ao terminar, treine em voz alta as perguntas PLENO da fase justificando trade-offs com o seu próprio código como exemplo.
4. Só avance de fase quando os critérios de pronto estiverem atendidos.

## Mapa fase → capítulos do banco

- Fase 1: 1 (Java Core), 2 (POO/SOLID), 3 (Spring), 4 (REST), 11 (fintech base)
- Fase 2: 5 (Segurança)
- Fase 3: 6 (Oracle/SQL), 7 (Redis), 4 (idempotência)
- Fase 4: 8 (Mensageria), 9 (Microsserviços/DDD), 11 (consistência)
- Fase 5: 10 (Docker/CI-CD/Git)
- Fase 6: 11 (alta volumetria)
- Capítulo 12 (comportamental): treine em paralelo, usando o próprio MiniVale como o "sistema que você desenvolveu e se orgulha".