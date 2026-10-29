# Fluxo: testes, schema, formatação, branches e commits

## Testes

Os testes de integração usam `@QuarkusTest` com RestAssured, ficam em
`src/test/java/dev/nipponten/` e são organizados por regra. Exemplos: `ProductSizeRulesTest`,
`ComboBusinessRulesTest`, `PrivilegeEscalationTest`.

- **Banco:** o Postgres sobe pelos Dev Services (Testcontainers), então o Docker precisa estar
  rodando.
- **Autenticação:** use `TestAuth.asAdmin()`, que é o super_admin com o token reutilizado, ou
  `TestAuth.as(token)` / `TestAuth.login(email, senha)` para testar 403. Chamada sem token usa
  `given()`.
- **Dados de apoio:** use `TestFixtures`, com `createProduct`, `createSize`, `createIngredient`,
  `createPromotionType` e `productRegistration`. Se o mesmo setup aparecer em mais de um teste,
  adicione um helper lá.
- **Banco compartilhado:** o banco é o mesmo para todas as classes de teste. Use nomes únicos
  (`"Medio-" + System.nanoTime()`) e filtre pelo próprio prefixo ao testar listagens.
- **Tempo:** quando a regra depende do relógio, use `TestClock` em vez de `sleep`.
- **Nomes de teste:** descrevem o comportamento em inglês, como
  `rejectsTheSameSizeTwiceOnTheSameProduct` e `refusesToDeleteASizeStillInUse`.

Comandos:

```shell
./mvnw test                         # suíte toda
./mvnw test -Dtest=CouponRulesTest  # uma classe
```

Se aparecer `Cannot connect to the Docker daemon` com o Docker rodando, o *context* ativo do Docker
aponta para outro socket. Rode com `DOCKER_HOST=unix:///var/run/docker.sock ./mvnw test`, ou troque
o context com `docker context use default`. Não altere a configuração do projeto por isso.

## Schema (Flyway)

- **Onde ficam:** as migrations estão em `src/main/resources/db/migration/V<n>__descricao.sql` e
  são aplicadas no boot (`quarkus.flyway.migrate-at-start=true`). O Hibernate fica em `validate`
  em todos os perfis.
- **Ao mudar uma entity:** crie uma migration nova e nunca altere uma já aplicada. Comece o arquivo
  com um comentário em português explicando o porquê.
- **Se a aplicação não sobe com erro de schema validation:** a entity e a migration não batem.
  Corrija a migration nova, sem desligar o `validate`.
- **Se encontrar `migrate-at-start` comentado** no `application.properties`: o boot quebra em
  banco novo e nos Dev Services. Avise o usuário. Na `main` a linha ainda está comentada; o commit
  `d854d16`, da `fix/resource-endpoints-review`, a restaura.

## Formatação

O projeto usa Spotless com google-java-format no estilo AOSP (indentação de 4) e remove imports não
usados. Antes de commitar:

```shell
./mvnw spotless:apply
```

Não misture reformatação de arquivos que você não tocou com a mudança funcional. Se precisar,
faça um commit `chore:` separado.

## Comentários no código

Os comentários são em português, curtos, e explicam o **porquê**, nunca o quê. Exemplos:
`// O preço vive no tamanho: remover o último deixaria o produto sem preço.` e
`// Repositório, e não o service, para não criar ciclo IngredientService <-> ProductService.`

Mensagens de exceção e de erro da API ficam em inglês.

## Branches

Use uma branch por tarefa, criada a partir de `main`: `feat/<assunto>`, `fix/<assunto>`,
`refactor/<assunto>` ou `docs/<assunto>`. A `develop` está atrasada em relação à `main` e não deve
ser usada como base.

## Commits

O padrão é Conventional Commits: **título em inglês, no imperativo, em minúsculas**, e **corpo em
português** explicando o problema e o porquê da solução.

```
fix: allow products to be registered without sizes

O preço vive no tamanho, mas exigir ao menos um na criação impedia
cadastrar um produto ainda sem preço definido. Agora `sizes` é
opcional, como `ingredients`, e o preço pode ser adicionado depois via
POST /products/{id}/sizes.
```

- **Tipos:** `feat`, `fix`, `refactor`, `docs`, `chore`, `build` e `test`. O escopo é opcional,
  como em `feat(auth):`.
- **Tamanho:** um commit por mudança lógica. Código, teste e doc da mesma mudança vão juntos.
- **Quando commitar:** só quando o usuário pedir.
