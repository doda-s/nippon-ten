---
name: nippon-ten-dev
description: Padrões de desenvolvimento do backend nippon-ten (Quarkus 3 / Java 25, Marusan). Use sempre que for criar ou alterar qualquer coisa neste repositório — endpoint/resource, service, regra de negócio, entidade, migration Flyway, permissão/segurança, listagem paginada, tratamento de erro, teste ou documentação em docs/ — mesmo que o usuário não peça explicitamente para "seguir os padrões". Também use ao revisar código ou planejar uma feature nova do nippon-ten (pedidos, cupons, pontos, etc.).
---

# nippon-ten: como desenvolver neste projeto

O nippon-ten é a API do sistema de pedidos da Marusan (pastelaria). A arquitetura é em camadas
(porta/adapter) e as regras de negócio estão documentadas em `docs/`. **`docs/` é a fonte da
verdade**: esta skill diz *onde* olhar e *como* trabalhar; a regra em si está na doc. Se a doc e o
código discordarem, avise o usuário em vez de escolher um lado em silêncio.

## 1. Antes de escrever código: leia a doc certa

| Vai mexer em… | Leia |
| --- | --- |
| Qualquer código (camadas, mappers, transação, validação, datas, auth no código) | `docs/architecture.md` |
| Quem pode chamar um endpoint, permissões, roles, super_admin, usuários | `docs/user-management.md` (seções *Acesso à API* e *Regras contra escalada de privilégio*) |
| Produto, tamanho, ingrediente, adicional, combo, promoção | `docs/products-and-promotions.md` |
| Contrato com o front (tokens, formato de erro, client npm) | `docs/api-client.md` |
| Feature nova — está no escopo? qual o número do requisito? | `docs/requirements.md` |
| Modelo de dados | `docs/data-modeling/data_modeling.drawio` |

Leia só a seção relevante, mas leia — várias regras não são óbvias (ex.: propagação de "esgotado"
é **manual por decisão de produto**; um `User` é cliente **ou** interno, nunca os dois).

## 2. Regras centrais (e por quê)

Pacote raiz `dev.nipponten`. Camadas de dentro para fora:

- **`domain`** — `models` (records puros, sem anotação de framework) e `repositories` (interfaces =
  portas). Não importa nada de fora. *Por quê:* trocar ORM/framework não pode tocar regra.
- **`application`** — `services`, `requests`/`responses` (+ mappers), `exceptions`. Depende só de
  `domain`.
- **`infrastructure`** — `entities` (JPA), `mappers` (Entity↔Model), `repositories`
  (`PanacheXRepository` implementa a porta), `security`, `bootstrap`, `time`.
- **`resource`** — endpoints JAX-RS, `resource.security.Permissions`, `resource.exception` (mappers
  de exceção). Depende de `application`/`domain`, **nunca** de `infrastructure`.

Convenções que a revisão vai cobrar:

1. **DTO nos dois sentidos.** `XRequest` + `XRequestMapper.toModel(id, request)` para entrar;
   `XResponseMapper.toResponse(model)` para sair. O `Resource` nunca faz `new X(...)` de domain
   model, e `XResponse` nunca expõe campo sensível.
2. **Filho só existe aninhado no pai.** Sem endpoint top-level (`/products/{id}/sizes`, não
   `/product-sizes`); `toModel(id, parentId, request)` com o pai vindo do path; o service do filho
   injeta o service do pai e passa tudo por `requireByParent(parentId, id)` (404 se o filho não for
   daquele pai). Na exclusão do pai: filhos próprios em cascata, referências de outro agregado
   bloqueiam com `InvalidRequestException` — consultando **repositórios** dos dependentes (services
   criariam ciclo).
3. **Todo id vindo no corpo** que aponta para outro agregado é validado via `getById` do service
   dono → 404, nunca erro de FK.
4. **Transação** fica no `PanacheXRepository.save/remove`. `@Transactional` no service só quando a
   operação cruza agregados (e aí documente o caso na seção *Transação* de `architecture.md`).
5. **Validação.** Formato de campo isolado = anotação Bean Validation no `Request` + `@Valid` no
   resource. `InvalidRequestException` (400) só para invariantes que dependem de outro campo/entidade/
   consulta. `XNotFoundException extends NotFoundException` (404) para id inexistente ou filho de
   outro pai.
6. **Segurança.** Toda rota tem anotação **no método** (`@PermitAll`, `@Authenticated` ou
   `@RolesAllowed(Permissions.X)`) — sem anotação = negado. Regra que depende de dados ("o próprio
   usuário ou `manage_users`", "ninguém concede o que não tem") vai no **service** via `CurrentUser`,
   nunca no resource. Endpoint novo entra na tabela *Acesso à API* de `user-management.md`.
7. **Datas.** Instante técnico = `Instant` UTC / `timestamptz`; horário de negócio (janela de
   combo/promoção) = `LocalDateTime` via `BusinessTime.now()`. "Agora" sempre do `Clock` injetado,
   nunca `now()` direto — senão o `TestClock` não controla o teste.
8. **Domain model sem invariantes.** Regra de negócio mora no service do agregado dono.

## 3. Padrões de listagem e de erro 400

O padrão oficial é o da branch `fix/adjust-ingredient-resource` (commit `1048b1e`):

- Listagem que pode crescer devolve `PageResponse<XResponse>` com
  `{items, page, size, totalItems, totalPages}`; query params `page` (0..), `size` (1..100, padrão
  20) e filtros opcionais; no domínio, `XFilter` (record em `domain/repositories`) → `Page<X>`
  (`domain/models`); ordenação estável terminando em `id`.
- Todo 400 sai em `ErrorResponse{message, errors[{field, message}]}` — `"Validation failed"`,
  `"Invalid request body"`, `"Malformed JSON request body"`, `"Invalid request parameter"` ou a
  mensagem da `InvalidRequestException`.
- Body obrigatório: `@NotNull(message = "request body is required") @Valid XRequest request`.

Detalhes e código de referência: **`references/listing-and-errors.md`**. Leia antes de criar ou
alterar qualquer `GET` de listagem ou mapper de exceção.

> ⚠️ A branch `fix/resource-endpoints-review` implementou outro formato (`content`/`totalElements`,
> `PageResult`, `getPage`, erro com `violations`). **Não siga esse formato.** Se o código que você
> está mexendo já tiver ele, avise o usuário do conflito em vez de misturar os dois.

## 4. Fluxo de trabalho

1. **Entender** — ler a doc (§1); se for feature nova, localizar o requisito em `requirements.md`.
   Se a regra não estiver documentada, pergunte ao usuário antes de inventar.
2. **Implementar** — seguir a receita arquivo a arquivo em **`references/new-resource-recipe.md`**
   (agregado top-level ou filho aninhado). Copie o estilo do vizinho mais parecido (Ingredient para
   agregado simples, ProductSize para filho).
3. **Schema** — toda mudança em entidade vem com migration Flyway nova
   `src/main/resources/db/migration/V<próximo>__descricao.sql`. O Hibernate está em `validate`:
   entidade sem migration = a aplicação não sobe. Nunca edite uma migration já existente.
4. **Testar** — `@QuarkusTest` + RestAssured cobrindo o caminho feliz e cada regra (404, 400, 403).
5. **Documentar** — atualizar `docs/` no mesmo commit (regra nova, endpoint, permissão, tabela de
   acesso, transação, mudança de modelo). Mudou o modelo de dados? Avise que o `.drawio` precisa ser
   atualizado manualmente.
6. **Formatar e verificar** — `./mvnw spotless:apply` e rodar os testes.
7. **Commitar** — Conventional Commits.

Detalhes de testes, Flyway, spotless, branches e commits: **`references/workflow.md`**.

## 5. Ao terminar, confira

- [ ] Nenhum import de `infrastructure` em `domain`/`application`/`resource`.
- [ ] Resource só monta request via mapper, chama service, devolve response via mapper.
- [ ] Toda rota com anotação de segurança no método; tabela *Acesso à API* atualizada.
- [ ] Ids do corpo validados no service; filho validado com `requireByParent`.
- [ ] Exclusão trata dependentes (cascata ou bloqueio).
- [ ] Listagem e erros no formato da §3.
- [ ] Migration Flyway nova, se a entidade mudou.
- [ ] Testes novos passando; `spotless:apply` rodado.
- [ ] `docs/` atualizado; divergências doc×código reportadas ao usuário.
