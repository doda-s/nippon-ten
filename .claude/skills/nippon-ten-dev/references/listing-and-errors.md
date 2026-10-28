# Listagem paginada e erros 400

Padrão oficial, vindo da branch `fix/adjust-ingredient-resource` (commit `1048b1e`). Quando essa
branch estiver na `main`, a seção *Validação* de `docs/architecture.md` e a de *Respostas de erro* de
`docs/api-client.md` passam a descrever o mesmo padrão, e elas viram a fonte da verdade. Até lá,
vale este arquivo. Para conferir o código original:
`git show 1048b1e:<caminho>`.

Hoje, só `GET /ingredients` segue o padrão. Listagem nova, ou listagem existente que você estiver
mexendo, deve segui-lo.

> **Não confundir** com `fix/resource-endpoints-review`, que usa `PageResult`, `getPage(page, size)`,
> `PageParams`, `{content, totalElements}` e erro `{message: "Invalid request", violations: [...]}`.
> Se você encontrar esse formato no código, avise o usuário: as duas branches vão conflitar no merge.

## Listagem

### Contrato HTTP

`GET /xs?page=0&size=20&<filtros>` devolve:

```json
{ "items": [ ... ], "page": 0, "size": 20, "totalItems": 57, "totalPages": 3 }
```

- `page` começa em `0`, com padrão `0`. `size` vai de `1` a `100`, com padrão `20`. Os filtros são
  todos opcionais e combináveis.
- A ordenação é fixa e termina em `id`. Sem `ORDER BY`, o Postgres não garante ordem, e o mesmo
  registro pode aparecer em duas páginas ou em nenhuma.
- Listas de filhos aninhados (`/products/{id}/sizes`) são pequenas e continuam como lista simples.

### Peças por camada

`domain/models/Page.java`:

```java
/** Uma página de resultados. {@code page} começa em 0. */
public record Page<T>(List<T> items, int page, int size, long totalItems) {
    public int totalPages() { return (int) ((totalItems + size - 1) / size); }
    public <R> Page<R> map(Function<T, R> mapper) {
        return new Page<>(items.stream().map(mapper).toList(), page, size, totalItems);
    }
}
```

`domain/repositories/XFilter.java`, com um record por recurso:

```java
/** Critérios opcionais da busca de ingredientes; {@code null} não filtra. */
public record IngredientFilter(String name, Ingredient.Status status) {}
```

Na porta do repositório, `Page<X> search(XFilter filter, int page, int size);` substitui
`List<X> getAll()`. O service só repassa: `return repository.search(filter, page, size);`.

Implementação Panache:

```java
@Override
public Page<Ingredient> search(IngredientFilter filter, int page, int size) {
    List<String> conditions = new ArrayList<>();
    Parameters parameters = new Parameters();
    if (filter.name() != null && !filter.name().isBlank()) {
        conditions.add("lower(name) like :name");
        parameters.and("name", "%" + filter.name().strip().toLowerCase(Locale.ROOT) + "%");
    }
    if (filter.status() != null) {
        conditions.add("status = :status");
        parameters.and("status", IngredientEntity.Status.valueOf(filter.status().name()));
    }
    // id desempata nomes iguais, para a ordem (e as páginas) não variar entre chamadas.
    Sort sort = Sort.by("name").and("id");
    PanacheQuery<IngredientEntity> query =
            conditions.isEmpty()
                    ? findAll(sort)
                    : find(String.join(" and ", conditions), sort, parameters);
    List<Ingredient> items =
            query.page(io.quarkus.panache.common.Page.of(page, size)).list().stream()
                    .map(mapper::toModel)
                    .toList();
    return new Page<>(items, page, size, query.count());
}
```

O nome `Page` colide com `io.quarkus.panache.common.Page`. Por isso, no repositório, o do Panache
é escrito com o nome qualificado.

`application/responses/PageResponse.java`:

```java
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.items(), page.page(), page.size(), page.totalItems(), page.totalPages());
    }
}
```

Resource:

```java
// Filtros opcionais e combináveis: name busca por trecho, sem diferenciar maiúsculas.
@GET
@PermitAll
public PageResponse<IngredientResponse> search(
        @QueryParam("name") String name,
        @QueryParam("status") Ingredient.Status status,
        @QueryParam("page") @DefaultValue("0") @PositiveOrZero int page,
        @QueryParam("size") @DefaultValue("20") @Min(1) @Max(100) int size) {
    return PageResponse.of(
            service.search(new IngredientFilter(name, status), page, size)
                    .map(mapper::toResponse));
}
```

Quando a listagem muda para paginada, documente os filtros na doc do recurso, como em
`docs/products-and-promotions.md`, seção Ingredient. Avise também que o formato da resposta mudou
para o front.

## Erros 400

Todos usam o mesmo corpo, `application/responses/ErrorResponse.java`:

```java
public record ErrorResponse(
        String message, @JsonInclude(JsonInclude.Include.NON_EMPTY) List<FieldError> errors) {
    public ErrorResponse(String message) { this(message, List.of()); }
    public record FieldError(String field, String message) {}
}
```

```json
{ "message": "Validation failed", "errors": [{ "field": "price", "message": "must be greater than or equal to 0" }] }
```

`field` é o caminho que o cliente enviou: `price`, `products[0].productId` ou o nome do query
param. Ele nunca é o caminho Java (`create.arg0.name`).

| `message` | Origem (`resource.exception`) |
| --- | --- |
| `Validation failed` | Bean Validation no corpo, query ou path param → `ConstraintViolationExceptionMapper`. Corpo ausente aparece como o campo `request`, desde que o parâmetro tenha `@NotNull`. |
| `Invalid request body` | JSON válido com valor que não cabe no campo (texto em número, enum inexistente) → `InvalidJsonValueExceptionMapper`. Para enum, lista os valores aceitos. |
| `Malformed JSON request body` | JSON sintaticamente inválido → `BadRequestWithoutBodyExceptionMapper`. |
| `Invalid request parameter` | Query ou path param que não converte. Para enum, `EnumParamConverterProvider`; para outros tipos, `BadRequestWithoutBodyExceptionMapper`. Sem esse mapper, o JAX-RS devolveria `404` sem corpo. |
| outras | `InvalidRequestException` lançada pelo service, com a mensagem da regra. |

Regras práticas:

- Todo body obrigatório recebe
  `@NotNull(message = "request body is required") @Valid XRequest request`.
- Não crie um mapper novo para 400. Se o caso não se encaixa na tabela acima, provavelmente é
  `InvalidRequestException` no service.
- Ao mudar o formato de erro, atualize a tabela de `architecture.md` e a seção *Respostas de erro*
  de `api-client.md`, que é o que o time do front lê.
- Teste o formato com RestAssured. Exemplo:
  `.body("message", is("Validation failed")).body("errors.field", hasItem("name"))`.
