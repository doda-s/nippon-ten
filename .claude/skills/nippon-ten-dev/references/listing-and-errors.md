# Listagem paginada e respostas de erro

Fonte da verdade: as seções *Paginação* e *Validação* de `docs/architecture.md` e *Respostas de
erro* de `docs/api-client.md`. Este arquivo resume o padrão e aponta o código de referência. Se o
código e a doc divergirem, avise o usuário.

## Listagem

### Contrato HTTP

Todo `GET` que lista um agregado top-level é paginado. `GET /xs?page=0&size=20` devolve:

```json
{ "content": [ ... ], "page": 0, "size": 20, "totalElements": 57, "totalPages": 3 }
```

- `page` começa em `0`, com padrão `0`. `size` tem padrão `20` e teto `100`.
- Valores fora da faixa **não** dão erro: `PageParams.of` normaliza (`page` negativo vira `0`;
  `size <= 0` vira `20`; `size > 100` vira `100`).
- A ordenação é `Sort.by("id")`. Sem `ORDER BY`, o Postgres não garante ordem, e o mesmo registro
  pode aparecer em duas páginas ou em nenhuma.
- Listas de filhos aninhados (`/products/{id}/sizes`) são pequenas e continuam como lista simples.

### Peças por camada

`domain/models/PageResult.java`, compartilhado por todos os recursos:

```java
public record PageResult<T>(List<T> content, long totalElements) {}
```

Na porta do repositório, `PageResult<X> getPage(int page, int size);`. Variantes filtradas ganham
um método próprio `getXxxPage(...)`, como `ComboRepository.getAvailablePage(LocalDateTime now, int
page, int size)`. O service só repassa:

```java
public PageResult<Ingredient> getPage(int page, int size) {
    return repository.getPage(page, size);
}
```

Implementação Panache (`PanacheIngredientRepository`):

```java
@Override
public PageResult<Ingredient> getPage(int page, int size) {
    PanacheQuery<IngredientEntity> query = findAll(Sort.by("id"));
    long total = query.count();
    List<Ingredient> content =
            query.page(Page.of(page, size)).list().stream().map(mapper::toModel).toList();
    return new PageResult<>(content, total);
}
```

Com filtro, use `find(query, Sort.by("id"), params...)` no lugar de `findAll`, como em
`PanacheComboRepository.getAvailablePage`.

`application/responses/PageResponse.java` monta o corpo e calcula `totalPages`:

```java
public record PageResponse<T>(
        List<T> content, int page, int size, long totalElements, int totalPages) {
    public static <S, T> PageResponse<T> of(
            PageResult<S> result, int page, int size, Function<S, T> mapper) { ... }
}
```

`resource/PageParams.java` normaliza os query params. Resource:

```java
@GET
@PermitAll
public PageResponse<IngredientResponse> getAll(
        @QueryParam("page") @DefaultValue("0") int page,
        @QueryParam("size") @DefaultValue("20") int size) {
    PageParams params = PageParams.of(page, size);
    return PageResponse.of(
            service.getPage(params.page(), params.size()),
            params.page(),
            params.size(),
            mapper::toResponse);
}
```

Filtro na listagem vira query param opcional que escolhe o método do service, como `available` em
`ComboResource.getAll`. Documente o filtro na doc do recurso e avise o front se o formato mudar.

## Respostas de erro

Dois corpos, ambos em `application/responses`:

- `ErrorResponse(String message)`, usado por todos os mappers exceto o de Bean Validation:

  ```json
  { "message": "Ingredient not found: 42" }
  ```

- `ValidationErrorResponse(String message, List<FieldViolation> violations)`, só para falha de
  Bean Validation:

  ```json
  { "message": "Invalid request", "violations": [{ "field": "price", "message": "must be greater than or equal to 0" }] }
  ```

  `field` é o último segmento do caminho da violação (`price`, e não `create.arg0.price`). As
  violações saem ordenadas por `field`.

| Status | Exceção | Mapper (`resource.exception`) |
| --- | --- | --- |
| `400` | `ConstraintViolationException` (`@Valid` no request) | `ConstraintViolationExceptionMapper` → `ValidationErrorResponse` |
| `400` | `InvalidRequestException` (regra do service) | `InvalidRequestExceptionMapper` |
| `401` | `UnauthorizedException` | `UnauthorizedExceptionMapper` |
| `401` | token ausente ou inválido (Quarkus) | `MissingCredentialsExceptionMapper`, mensagem `Invalid or expired token` |
| `403` | `ForbiddenException` | `ForbiddenExceptionMapper` |
| `404` | `NotFoundException` e subclasses `XNotFoundException` | `GlobalExceptionMapper` |
| `429` | `TooManyRequestsException` | `TooManyRequestsExceptionMapper`, com header `Retry-After` |

Regras práticas:

- Body de request recebe `@Valid XRequest request`. Formato de campo isolado é anotação no
  `Request`; regra que depende de outro campo, entidade ou consulta é `InvalidRequestException` no
  service.
- Não crie mapper novo para 400. Se o caso não se encaixa na tabela, provavelmente é
  `InvalidRequestException`.
- Ao mudar um formato de erro, atualize a seção *Validação* de `architecture.md` e *Respostas de
  erro* de `api-client.md`, que é o que o time do front lê.
- Teste o formato com RestAssured, como em `ValidationErrorFormatTest`:
  `.body("message", is("Invalid request")).body("violations.field", hasItem("name"))`.
