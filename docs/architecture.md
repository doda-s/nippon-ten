## Camadas

O código está organizado em quatro pacotes sob `dev.nipponten`, de dentro para fora:

- `domain` — `models` (records Java puros, sem anotação de framework) e `repositories` (as *portas*: interfaces que o domínio exige para persistir e recuperar seus dados, sem saber como isso é feito).
- `application` — `services` (orquestração e regras de aplicação), `requests`/`responses` (DTOs de entrada e saída) e `exceptions` (erros de domínio/aplicação).
- `infrastructure` — `entities` (JPA/Panache), `mappers` (Entity ↔ Domain Model) e `repositories` (as *implementações* Panache das portas do domínio).
- `resource` — os endpoints REST (Quarkus/JAX-RS) e o mapeamento de exceção para resposta HTTP (`resource.exception`).

O domínio nunca importa nada de `infrastructure` ou `resource`. `application` depende só de `domain`. `resource` depende de `application` e `domain`, nunca diretamente de `infrastructure`.

---

## Repositório: porta no domínio, adapter na infraestrutura

Cada agregado tem uma interface de repositório em `domain.repositories` (ex.: `ProductRepository`) com uma API estável e agnóstica de ORM:

```java
public interface ProductRepository {
    Product save(Product model);
    void remove(Product model);
    Product getById(Long id);
    List<Product> getAll();
}
```

A implementação vive em `infrastructure.repositories` como um `PanacheXRepository`, que implementa a interface do domínio e o `PanacheRepository<XEntity>` do Quarkus ao mesmo tempo. É essa classe que sabe converter entre `Entity` e `Model` (via o `Mapper` correspondente) e que carrega `@Transactional` nas operações de escrita de um único agregado (`save`/`remove`).

Isso mantém o domínio livre de Panache: se um dia o ORM mudar, só `infrastructure` muda.

---

## Request/Response: DTO dedicado nos dois sentidos

Todo agregado exposto por REST tem, em `application.requests`/`application.responses`:

- `XRequest` — o corpo aceito pelo endpoint, anotado com Bean Validation.
- `XRequestMapper` — converte `XRequest` (+ id, + id do pai quando aplicável) em `X` (domain model). Único lugar do sistema que monta um domain model a partir de um request.
- `XResponse` — o corpo devolvido pelo endpoint. Nunca inclui campos sensíveis (ex.: `User` não expõe `password`).
- `XResponseMapper` — converte `X` (domain model) em `XResponse`.

O `Resource` nunca constrói um domain model manualmente (`new Product(...)`); sempre via `XRequestMapper.toModel(id, request)`. Isso é simétrico: assim como existe um mapper para sair do domínio, existe um para entrar.

---

## Agregados e seus filhos: sempre aninhado, sem repositório "solto"

Quando uma entidade só faz sentido no contexto de outra (ex.: `ProductSize`, `ProductIngredient` e `AdditionalIngredient` dentro de `Product`; `ComboProduct` dentro de `Combo`), ela:

- **não tem endpoint top-level.** É acessada exclusivamente por rotas aninhadas sob o pai: `/products/{id}/sizes`, `/combos/{id}/products`, etc.
- **tem `RequestMapper` com a assinatura `toModel(Long id, Long parentId, XRequest request)`**, porque o id do pai vem do path, nunca do corpo.
- **tem o Service do filho injetando o Service do pai**, para validar a existência do pai e a posse do filho.

O padrão de validação em todo Service de filho é:

```java
public X create(X model) {
    parentService.getById(model.parentId()); // 404 se o pai não existe
    return repository.save(model);
}

public X requireByParent(Long parentId, Long id) {
    parentService.getById(parentId);
    X model = getById(id);
    if (!parentId.equals(model.parentId())) {
        throw new XNotFoundException(id); // 404 se o filho não pertence a esse pai
    }
    return model;
}
```

`update`/`delete` sempre passam por `requireByParent` antes de tocar o repositório. O `Resource` correspondente não faz nenhuma dessas checagens — só recebe os path params, monta o request via mapper e chama o service.

`User` segue a mesma lógica para `Client` e `UserAddress` (filhos de `User`/`Client`), com `UserAddressService` dependendo de `ClientService` da mesma forma.

`Internal` também é filho de `User`, mas é gerenciado por `/internal` (o dashboard lista usuários internos, não usuários): `POST /internal` cria `User` + `Internal` de uma vez, `PUT /internal/{id}` nunca troca o `userId` e `DELETE /internal/{id}` remove o `Internal` junto com o `User`. Um `User` é criado já como cliente (`POST /users`) ou já como interno (`POST /internal`) — nunca os dois.

### Referências e exclusão

Todo id que chega no **corpo** do request e aponta para outro agregado (`ingredientId`, `productId`, `promotionTypeId`, `internalRoleId`) é validado no `Service` via `getById` do service dono antes de salvar — id inexistente é `404`, nunca erro de FK.

Na exclusão de um pai, o `Service` do pai consulta os **repositórios** dos dependentes (não os services, que já injetam o service do pai e criariam ciclo):

- **filhos do próprio agregado são removidos em cascata**: `Product` → `ProductSize`, `ProductIngredient`, `AdditionalIngredient`; `Combo` → `ComboProduct`; `User` → `UserAddress`, `Client`, `Internal`;
- **referências vindas de outro agregado bloqueiam a exclusão** com `InvalidRequestException` (`400`): `Product` usado por combo ou promoção; `Ingredient` usado por produto ou adicional; `PromotionType` usado por promoção; `InternalRole` atribuída a usuário interno.

---

## Transação: no repositório por padrão, no service quando cruza agregados

Toda escrita de um único agregado é transacional no nível do repositório Panache (`PanacheXRepository.save`/`remove`). Isso é suficiente para a maioria dos casos.

Quando um `Service` precisa orquestrar **mais de um agregado em sequência** dentro da mesma operação — hoje isso acontece em `UserService.register` (cria `User` + `Client` + `UserAddress` opcional), `UserService.registerInternal` (cria `User` + `Internal`), `UserService.delete`/`deleteInternal` (remove `UserAddress`(s) + `Client` + `Internal` + `User`), `ProductService.delete` (remove os filhos do produto + `Product`), `ComboService.register` (cria `Combo` + os `ComboProduct`(s) do fluxo de criação) e `ComboService.delete` (remove `ComboProduct`(s) + `Combo`) — o método do `Service` também leva `@Transactional`, garantindo que a operação inteira seja atômica. Fora desses casos, não se adiciona `@Transactional` no nível de `Service`.

---

## Validação: Bean Validation nos requests, exceção de domínio para o resto

Todo `XRequest` é anotado com Bean Validation (`@NotNull`, `@NotBlank`, `@Positive`, `@Email` etc.) e todo método de `Resource` que recebe um request usa `@Valid`. A extensão `quarkus-hibernate-validator` intercepta antes do request chegar ao `Service` e devolve `400` automaticamente se alguma constraint falhar.

`InvalidRequestException` (mapeada para `400` por `InvalidRequestExceptionMapper`) fica reservada para regras que Bean Validation não consegue expressar — invariantes que dependem de mais de um campo, de outra entidade ou de uma consulta (ex.: "um combo precisa ter no mínimo dois produtos"). Se a regra é sobre a forma de um campo isolado, ela é uma anotação no `Request`, não uma checagem manual no `Service`.

`XNotFoundException` (todas estendendo `NotFoundException`, mapeada para `404` por `GlobalExceptionMapper`) é usada para "esse id não existe" ou "esse filho não pertence a esse pai" — nunca para validação de formato de request.

---

## O que ainda não existe (fora do escopo desta documentação)

Os documentos [`requirements.md`](requirements.md) e [`user-management.md`](user-management.md) descrevem um sistema de permissões por cargo que ainda não tem código correspondente: o catálogo fixo de `permission`, a relação N:N `role_permission`, a role reservada `super_admin` (e o seed de bootstrap que a cria junto do primeiro usuário interno) e autenticação/autorização de fato. Hoje `InternalRole` é só `(id, name)` e todos os endpoints são abertos.

Os domain models são apenas dados (records sem invariantes): as regras de negócio vivem no `Service` do agregado responsável, seguindo os padrões descritos acima. É lá que estão, por exemplo, o mínimo de dois produtos por combo, a promoção única por produto e a validação das janelas de disponibilidade.

A propagação de status "esgotado" entre ingrediente → produto → combo/promoção continua sendo **manual por decisão de produto**, não uma lacuna: `GET /ingredients/{id}/dependents` e `GET /products/{id}/dependents` devolvem quem depende do item para o usuário interno decidir, e o sistema nunca altera esses status sozinho.
