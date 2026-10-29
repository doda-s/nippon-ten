# Listagem e respostas de erro

Fonte da verdade: a seção *Validação* de `docs/architecture.md` e *Respostas de erro* de
`docs/api-client.md`. Este arquivo resume o padrão e aponta o código de referência. Se o código e a
doc divergirem, avise o usuário.

## Listagem

Hoje nenhuma listagem é paginada. `GET /xs` devolve um array JSON simples com todos os registros.

- Na porta do repositório: `List<X> getAll();`. Variantes filtradas ganham um método próprio, como
  `ComboRepository.getAvailable(LocalDateTime now)`.
- O service só repassa: `return repository.getAll();`.
- Resource:

  ```java
  @GET
  @PermitAll
  public List<IngredientResponse> getAll() {
      return service.getAll().stream().map(mapper::toResponse).toList();
  }
  ```

- Filtro na listagem vira query param opcional que escolhe o método do service, como `available`
  em `ComboResource.getAll`.
- Listas de filhos aninhados (`/products/{id}/sizes`) seguem o mesmo formato.

Paginação é uma mudança de contrato com o front. Não pagine uma listagem por conta própria:
pergunte ao usuário e, se for feita, documente o padrão em `architecture.md` e em `api-client.md`.

## Respostas de erro

Todos os mappers de `resource.exception` devolvem `application/responses/ErrorResponse`:

```java
public record ErrorResponse(String message) {}
```

```json
{ "message": "Ingredient not found: 42" }
```

| Status | Exceção | Mapper (`resource.exception`) |
| --- | --- | --- |
| `400` | `InvalidRequestException` (regra do service) | `InvalidRequestExceptionMapper` |
| `401` | `UnauthorizedException` | `UnauthorizedExceptionMapper` |
| `401` | token ausente ou inválido (Quarkus) | `MissingCredentialsExceptionMapper`, mensagem `Invalid or expired token` |
| `403` | `ForbiddenException` | `ForbiddenExceptionMapper` |
| `404` | `NotFoundException` e subclasses `XNotFoundException` | `GlobalExceptionMapper` |
| `429` | `TooManyRequestsException` | `TooManyRequestsExceptionMapper`, com header `Retry-After` |

A falha de Bean Validation (`@Valid` no request) **não** tem mapper próprio. Ela sai com o `400`
padrão do `quarkus-hibernate-validator`, que é verboso e usa o caminho Java do campo
(`create.arg0.name`), não o formato `ErrorResponse`.

Regras práticas:

- Body de request recebe `@Valid XRequest request`. Formato de campo isolado é anotação no
  `Request`. Regra que depende de outro campo, entidade ou consulta é `InvalidRequestException` no
  service.
- Não crie mapper novo para 400 sem combinar com o usuário. Se o caso não se encaixa na tabela,
  provavelmente é `InvalidRequestException`.
- Ao mudar um formato de erro, atualize a seção *Validação* de `architecture.md` e *Respostas de
  erro* de `api-client.md`, que é o que o time do front lê.
- Teste o corpo com RestAssured: `.statusCode(404).body("message", is("Ingredient not found: " + id))`.
