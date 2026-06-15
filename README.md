# MiniVale — Carteira de Benefícios e Pagamentos

API REST de uma carteira digital de benefícios (estilo VR/VA/VT): usuários têm contas com saldo, transferem valores entre si confirmando por OTP, e consultam saldo e extrato. Tudo autenticado com JWT.

É um projeto de estudo que estou construindo em fases, mas não é "código de tutorial": cada decisão aqui foi tomada pensando em como eu defenderia ela numa entrevista técnica de backend Java pleno. Por isso este README explica *por que* as coisas estão do jeito que estão, e não só *o que* elas fazem.

> **Onde estamos hoje:** Fases 1 e 2 concluídas (núcleo da carteira + segurança/2FA). A próxima é a Fase 3 (Redis, cache e SQL avançado). O roteiro completo das 6 fases está no [`PROJECT.md`](./PROJECT.md).

---

## Stack atual

| Camada | Tecnologia | Por quê |
|---|---|---|
| Linguagem | Java 17 | Records para DTOs, baseline LTS |
| Framework | Spring Boot 4.1 (Web MVC, Data JPA, Security, Validation) | Padrão de mercado para API REST corporativa |
| Persistência | Spring Data JPA + Hibernate | Mapeamento objeto-relacional sem boilerplate de SQL |
| Banco | H2 em memória | **Temporário** — só para desenvolver rápido sem subir infra. Migra para Postgres/Oracle na Fase 3 |
| Segurança | Spring Security + JWT (jjwt 0.12.6) | Autenticação stateless |
| Hash | BCrypt | Senhas e códigos OTP |
| Build | Maven | — |
| Testes | JUnit 5 + Mockito + spring-security-test | Testes unitários de serviço |

O banco H2 é uma escolha consciente de *fase*: ele me deixa focar no domínio sem gastar tempo com Docker/Oracle agora. O preço é que os dados somem a cada restart (`ddl-auto=update`, `jdbc:h2:mem`). Quando isso virar um problema real (cache, índices, window functions), troco — e essa troca é justamente o começo da Fase 3.

---

## Como rodar

```bash
# subir a aplicação
./mvnw spring-boot:run

# rodar os testes
./mvnw test
```

A aplicação sobe em `http://localhost:8080`. O console do H2 fica em `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:minivale`, user `sa`, senha em branco) — liberado sem token só no ambiente local.

Já existem dois usuários carregados via `data.sql` (mesma senha BCrypt para ambos, `senha123`):

| Email | Papel |
|---|---|
| `ana.silva@email.com` | `ADMIN` |
| `bruno.costa@email.com` | `USER` |

### Fluxo completo de ponta a ponta

```bash
# 1. login -> recebe accessToken + refreshToken
curl -X POST localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"bruno.costa@email.com","senha":"senha123"}'

# 2. criar conta (passe o accessToken no header das próximas chamadas)
curl -X POST localhost:8080/contas \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"usuarioId":2,"saldoInicial":100.00}'

# 3. iniciar transferência -> responde 202 + o código OTP
curl -X POST localhost:8080/contas/transferencias \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"contaOrigemId":1,"contaDestinoId":2,"valor":30.00}'

# 4. confirmar com o OTP -> só agora o saldo se move
curl -X POST localhost:8080/contas/transferencias/1/confirmar \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"codigo":"123456"}'
```

> Em produção o OTP iria por um canal separado (SMS/app). Aqui ele volta no corpo da resposta do passo 3 só para o fluxo ser testável sem mock de SMS. Isso está marcado como simplificação consciente, não como "feito".

---

## Endpoints

| Método | Rota | O que faz | Autenticação |
|---|---|---|---|
| `POST` | `/auth/login` | Login, devolve access + refresh token | Pública |
| `POST` | `/auth/refresh` | Rotaciona o refresh token e emite novo par | Pública (valida o token no corpo) |
| `POST` | `/auth/logout` | Revoga o refresh token | Pública (valida o token no corpo) |
| `POST` | `/contas` | Cria conta para um usuário | Token *(ver débito técnico abaixo)* |
| `GET` | `/contas/{id}` | Consulta saldo | Dono da conta ou ADMIN |
| `GET` | `/contas/{id}/extrato` | Lista transações (mais recentes primeiro) | Dono da conta ou ADMIN |
| `POST` | `/contas/transferencias` | Inicia transferência, gera OTP, responde `202` | Dono da conta de origem ou ADMIN |
| `POST` | `/contas/transferencias/{id}/confirmar` | Valida OTP e efetiva o débito/crédito | Dono da transferência ou ADMIN |

---

## Decisões de design (o "porquê")

### 1. Dinheiro é `BigDecimal`, nunca `double`

`double` e `float` são ponto flutuante binário — não conseguem representar `0,10` exatamente, e o erro acumula em somas. Para dinheiro isso é inaceitável. Uso `BigDecimal` com `precision = 19, scale = 2` em todas as colunas monetárias e comparo valores com `compareTo` (e não `equals`, que considera `2.0` ≠ `2.00`).

### 2. Modelo de domínio rico, não anêmico

`Conta` não é um saco de getters/setters. Ela protege o próprio invariante: `debitar()` rejeita valor não-positivo e lança `SaldoInsuficienteException` antes de deixar o saldo ficar negativo; `creditar()` rejeita valor não-positivo. A regra "o saldo nunca pode ficar negativo" mora *dentro* da entidade, não espalhada pelo service. O service orquestra; a entidade garante a consistência dela mesma.

```java
public void debitar(BigDecimal valor) {
    if (valor.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException(...);
    if (valor.compareTo(this.saldo) > 0) throw new SaldoInsuficienteException(...);
    this.saldo = this.saldo.subtract(valor);
}
```

### 3. Transferência atômica com `@Transactional` + ledger append-only

`confirmarTransferencia()` é `@Transactional`: ou debita a origem **e** credita o destino **e** grava a `Transacao`, ou nada acontece. Não existe estado em que o dinheiro "sumiu no meio".

O `Transacao` é um **ledger append-only**: cada movimentação efetivada vira um registro imutável (sem setters, só construtor). Não atualizo "o saldo histórico" — eu registro o evento. É como sistemas financeiros de verdade funcionam, e é o que torna o extrato auditável.

### 4. Concorrência: locking otimista com `@Version`

`Conta` tem um campo `@Version`. Se duas transferências tentarem mexer na mesma conta ao mesmo tempo, a segunda a commitar leva `OptimisticLockingFailureException`, que o handler global traduz para `409 Conflict` com a mensagem "tente novamente".

Escolhi otimista (e não pessimista/`SELECT FOR UPDATE`) porque conflito de escrita na mesma conta é a exceção, não a regra — não vale a pena segurar lock de banco no caminho feliz. Sob alta contenção a conta seria diferente, e isso é uma discussão honesta para a Fase 6.

### 5. OTP como entidade separada do ledger (o ponto mais importante da Fase 2)

Esse foi o desenho que mais pensei. A transferência tem **dois passos** e um estado intermediário, e eu não queria que uma transferência "talvez nunca confirmada" poluísse o ledger.

```
POST /contas/transferencias          -> cria TransferenciaPendente (202 Accepted)
                                          guarda hash do OTP + expiraEm + confirmada=false
                                          NÃO mexe no saldo ainda

POST /contas/transferencias/{id}/confirmar
                                       -> valida OTP -> SÓ AQUI debita/credita e grava Transacao
```

Por que separar `TransferenciaPendente` de `Transacao`:

- **O ledger só registra fato consumado.** Uma intenção pendente não é uma movimentação.
- **Adia a validação de saldo para a confirmação**, mitigando uma janela TOCTOU (time-of-check / time-of-use) entre iniciar e confirmar. O saldo é verificado no momento que importa.
- **Replay attack é bloqueado de duas formas:** a flag `confirmada` torna o OTP de uso único, e `expiraEm` (TTL de 5 min) o invalida no tempo. Tentar confirmar duas vezes, ou tarde demais, dá `401`.

### 6. OTP é tratado como senha: guardado só em hash

Nunca persisto o código OTP em texto puro. O `OtpService` gera 6 dígitos com `SecureRandom` (não `Random` — previsibilidade em código de segurança é falha), guarda só o **hash BCrypt** e valida com `passwordEncoder.matches()`. Mesmo quem ler a tabela `transferencias_pendentes` no banco não consegue reverter o código.

### 7. JWT stateless + refresh token rotativo

- **Access token (JWT, HS256, 15 min):** curto de propósito. Carrega só `subject` (email) e `authorities` — nada sensível, porque o payload de um JWT é apenas base64, qualquer um lê.
- **Refresh token (UUID opaco, 7 dias, no banco):** é o que permite revogar. Diferente do access token (que é stateless e válido até expirar), o refresh vive numa tabela e pode ser invalidado.
- **Rotação a cada uso:** quando você dá `/refresh`, o token antigo é **revogado** e um novo par é emitido. Se um refresh token vazar e for usado, o legítimo para de funcionar e o roubo fica detectável. Logout simplesmente revoga o refresh token.

A API é `STATELESS` (sem `HttpSession`). Por isso CSRF está desabilitado conscientemente: o ataque clássico de CSRF depende de cookie de sessão enviado automaticamente pelo navegador, e aqui não existe cookie de sessão — a credencial vai no header `Authorization`, que não é anexado sozinho.

### 8. Autorização por dono do recurso, não só por papel

Não basta estar autenticado — você só acessa **a sua** conta. Isso é feito com `@PreAuthorize` chamando um bean `ContaSecurity`:

```java
@PreAuthorize("hasRole('ADMIN') or @contaSecurity.isOwner(#id, authentication)")
```

`ADMIN` vê tudo; `USER` só o que é dele. Isso ataca diretamente o **BOLA / IDOR** (OWASP API1:2023) — o usuário não consegue trocar o `{id}` da URL e ler a conta do vizinho. A verificação de "dono" mora num componente isolado e testável, fora do controller.

### 9. Contrato de erro padronizado (RFC 7807)

Todo erro sai como `ProblemDetail` (problem+json), centralizado num `@RestControllerAdvice`. Cada exceção de domínio mapeia para o status HTTP semanticamente correto:

| Exceção | HTTP | Significado |
|---|---|---|
| `RecursoNaoEncontradoException` | `404` | Conta/usuário não existe |
| `SaldoInsuficienteException` | `422` | Requisição válida, regra de negócio barrou (+ detalhes do saldo no corpo) |
| `TransferenciaInvalidaException` | `422` | Ex.: origem == destino |
| `UsuarioJaPossuiContaException` | `409` | Conflito de estado |
| `OptimisticLockingFailureException` | `409` | Concorrência |
| `BadCredentialsException` / `TokenInvalidoException` / `OtpInvalidoException` | `401` | Falha de autenticação |
| `AccessDeniedException` | `403` | Autenticado, mas sem permissão |
| `MethodArgumentNotValidException` | `400` | Bean Validation falhou (+ mapa campo→erro) |

A distinção `400` vs `422` é proposital: `400` é "você mandou errado" (validação de formato); `422` é "entendi seu pedido, mas a regra de negócio não deixa".

### 10. Injeção por construtor, sempre

Toda dependência entra por construtor (nunca `@Autowired` em campo). Deixa os campos `final`, deixa óbvio quando uma classe tem dependências demais (cheiro de violação de SRP), e torna o teste unitário trivial — é só passar mocks no construtor, sem precisar de contexto Spring.

---

## Como está testado

Os testes são unitários de serviço, com Mockito (sem subir banco):

- `ContaServiceTest` — criação de conta, transferência, saldo insuficiente, conta inexistente, origem == destino, fluxo de OTP.
- `AuthServiceTest` — login, rotação de refresh token, logout, token inválido/expirado.
- `OtpServiceTest` — geração, formato de 6 dígitos, hash e validação.
- `JwtServiceTest` — geração e parsing do token, expiração, assinatura.

```bash
./mvnw test
```

A estratégia aqui é testar a *lógica de negócio* isolada. Testes de integração com `@SpringBootTest`/Testcontainers entram quando houver banco real para valer a pena (Fase 3+).

---

## Débitos técnicos conhecidos (sendo honesto)

Prefiro deixar isso explícito a esconder. São pontos que conheço e ainda não corrigi, com a razão:

1. **`401` vs `403` em requisição não autenticada.** Bater num endpoint protegido sem token devolve `403 Forbidden`, quando o correto seria `401 Unauthorized`. É o comportamento padrão do Spring Security para API stateless; corrigir exige um `AuthenticationEntryPoint` customizado. Está na fila.

2. **`POST /contas` aceita `usuarioId` no corpo e não tem `@PreAuthorize`.** Isso é um vetor de **BOLA** — em tese eu poderia criar conta para outro usuário. O certo é derivar o usuário do *principal* autenticado, não confiar no payload. É a próxima coisa a endurecer.

3. **Segredo JWT versionado no `application.properties`.** Ok para estudo local, inaceitável em produção — vai para variável de ambiente/secret manager na Fase 5 (Docker/CI-CD).

4. **OTP retornado na resposta HTTP.** Simplificação para testar sem canal de SMS. Em produção o código nunca volta na mesma resposta que o inicia.

---

## Roadmap

- ✅ **Fase 1 — Núcleo da carteira:** conta, saldo, transferência atômica, ledger, locking otimista, tratamento de erro RFC 7807, validação.
- ✅ **Fase 2 — Segurança:** JWT, refresh token rotativo, logout/revogação, OTP/2FA na transferência, autorização por dono do recurso, BCrypt.
- ⏭️ **Fase 3 — Performance:** Redis (cache-aside de saldo), rate limiting, idempotency key na transferência, extrato com saldo acumulado via window function, índices, migração para banco persistente.
- 🔜 **Fase 4 — Distribuído:** mensageria (Kafka/RabbitMQ), microsserviços, Saga + compensação, Transactional Outbox, Resilience4j.
- 🔜 **Fase 5 — Infra:** Docker multi-stage, docker-compose, pipeline CI/CD com gates de qualidade, gestão de segredos.
- 🔜 **Fase 6 (opcional) — Alta volumetria:** fechamento de lote idempotente, estorno, reconciliação, simulação de pico.

O detalhamento de cada fase — conceitos exercitados, critérios de pronto e mapa para os capítulos de estudo — está no [`PROJECT.md`](./PROJECT.md).
