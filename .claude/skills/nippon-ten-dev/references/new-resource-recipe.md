# Receita: novo agregado ou novo filho aninhado

Siga a ordem abaixo, de dentro para fora. Para cada arquivo, abra o vizinho de referência e copie o
estilo: anotações, injeção por campo com `@Inject` e mensagens em inglês.

- **Agregado top-level simples:** `Ingredient`.
- **Filho aninhado:** `ProductSize`, com o pai `Product`.
- **Criação atômica de pai com filhos:** `ProductService.register` e `ComboService.register`.

Exemplo de nomes usado abaixo: `Coupon`. Pacote raiz: `src/main/java/dev/nipponten`.

## 1. `domain/models/Coupon.java`: record puro

```java
public record Coupon(Long id, String code, BigDecimal value, LocalDateTime startDate,
        LocalDateTime endDate, Status status) {
    public enum Status { ACTIVE, INACTIVE }
}
```

- Não leva anotação de framework nem invariante, porque a regra mora no service.
- No filho, o id do pai é um campo, por exemplo `Long productId`.
- Horário de negócio usa `LocalDateTime`. Instante técnico usa `Instant`. Veja *Datas* em
  `architecture.md`.

## 2. `domain/repositories/CouponRepository.java`: a porta

```java
public interface CouponRepository {
    Coupon save(Coupon model);
    void remove(Coupon model);
    Coupon getById(Long id);
    List<Coupon> getAll(); // listagem: ver listing-and-errors.md
}
```

- No filho, adicione `List<X> getByParent(Long parentId)`, como `getByProduct`.
- Se outro agregado precisa saber se este é usado, adicione uma consulta específica, como
  `getByIngredient`.

## 3. `infrastructure/entities/CouponEntity.java`

- `@Entity @Table(name = "coupon")`, `@Id @GeneratedValue private Long id;` e getters e setters
  explícitos, sem Lombok.
- O enum é duplicado dentro da entity (`CouponEntity.Status`), com `@Enumerated(EnumType.STRING)`,
  para o domínio não depender de JPA.
- Colunas em snake_case, geradas automaticamente pela physical naming strategy.

## 4. `infrastructure/mappers/CouponMapper.java`

`@ApplicationScoped`, com os métodos `toEntity(model)`, `updateEntity(entity, model)`,
`toModel(entity)` e um `applyToEntity` privado compartilhado. Enums são convertidos com
`X.valueOf(y.name())`.

## 5. `infrastructure/repositories/PanacheCouponRepository.java`

```java
@ApplicationScoped
public class PanacheCouponRepository implements CouponRepository, PanacheRepository<CouponEntity> {
    @Inject CouponMapper mapper;

    @Override @Transactional
    public Coupon save(Coupon model) {
        CouponEntity entity;
        if (model.id() == null) { entity = mapper.toEntity(model); persist(entity); }
        else { entity = findById(model.id()); mapper.updateEntity(entity, model); }
        return mapper.toModel(entity);
    }

    @Override @Transactional
    public void remove(Coupon model) { delete(findById(model.id())); }

    @Override
    public Coupon getById(Long id) {
        CouponEntity entity = findById(id);
        return entity == null ? null : mapper.toModel(entity);
    }
}
```

`@Transactional` só fica em `save` e `remove`. O `getById` do repositório devolve `null`, e quem
lança o 404 é o service.

## 6. Migration Flyway

Crie `src/main/resources/db/migration/V<próximo número>__coupon.sql`. Para saber o próximo número,
liste a pasta. Comece com um comentário em português explicando o porquê da migration, como em
`V3__internal_role_reserved.sql`. Use `bigserial`/`bigint` para ids e FKs para o pai, `timestamptz`
para instantes e `timestamp` para horário de negócio.

O Hibernate roda em `validate`. Se a entity e a migration não baterem, a aplicação não sobe. Por
isso:

- não edite migration que já existe;
- não troque a estratégia para `update`.

## 7. `application/exceptions/CouponNotFoundException.java`

```java
public class CouponNotFoundException extends NotFoundException {
    public CouponNotFoundException(Long id) { super("Coupon not found: " + id); }
}
```

## 8. Request e RequestMapper (`application/requests`)

- `CouponRequest` é um record com Bean Validation em cada campo: `@NotBlank`, `@NotNull`,
  `@Positive` e assim por diante.
- Nunca coloque `id` no body, nem o id do pai.
- `CouponRequestMapper.toModel(Long id, CouponRequest request)`.
- No filho, a assinatura é `toModel(Long id, Long parentId, XRequest request)`.

## 9. Response e ResponseMapper (`application/responses`)

`CouponResponse` é um record sem campos sensíveis. `CouponResponseMapper.toResponse(Coupon)` é
`@ApplicationScoped`.

## 10. Service (`application/services/CouponService.java`)

```java
@ApplicationScoped
public class CouponService {
    @Inject CouponRepository repository;

    public Coupon create(Coupon model) { /* regras + validar ids do corpo via outros services */ return repository.save(model); }

    public Coupon getById(Long id) {
        Coupon model = repository.getById(id);
        if (model == null) throw new CouponNotFoundException(id);
        return model;
    }

    public Coupon update(Long id, Coupon model) { getById(id); return repository.save(model); }

    public void delete(Long id) { /* bloquear se referenciado por outro agregado; cascata dos filhos */ repository.remove(getById(id)); }
}
```

**Filho:** injeta o service do pai e usa o padrão abaixo, como em `ProductSizeService`.

```java
public X create(X model) { parentService.getById(model.parentId()); return repository.save(model); }

public X requireByParent(Long parentId, Long id) {
    parentService.getById(parentId);
    X model = getById(id);
    if (!parentId.equals(model.parentId())) throw new XNotFoundException(id);
    return model;
}
// update(parentId, id, model) e delete(parentId, id) passam primeiro por requireByParent.
```

Pontos de atenção no service:

- **Ciclos de dependência.** Para consultar dependentes, injete o *repositório* do outro agregado,
  não o service. Deixe um comentário explicando o motivo, como em `IngredientService`.
- **Regra de negócio.** Use `InvalidRequestException` com a mensagem em inglês, explicando o que
  foi recusado. Comente em português o *porquê* da regra.
- **Regra que depende de quem chama.** Injete `CurrentUser` e use `requireSelfOr(...)` ou
  `requireHolds(...)`.
- **Operação que cruza agregados.** Coloque `@Transactional` no método do service e liste o caso na
  seção *Transação* de `architecture.md`.
- **"Agora".** Para "agora", injete o `Clock` e use `BusinessTime.now()` ou `Instant.now(clock)`.

## 11. Resource (`resource/CouponResource.java`)

```java
// Leitura pública; escrita exige a permissão. As anotações ficam nos métodos para o OpenAPI marcar
// só as rotas protegidas.
@Path("/coupons")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CouponResource {
    @Inject CouponService service;
    @Inject CouponRequestMapper requestMapper;
    @Inject CouponResponseMapper mapper;

    @POST
    @RolesAllowed(Permissions.MANAGE_PROMOTIONS)
    public Response create(@Valid CouponRequest request) {
        Coupon saved = service.create(requestMapper.toModel(null, request));
        return Response.status(Response.Status.CREATED).entity(mapper.toResponse(saved)).build();
    }
    // GET /{id} → 200; GET (listagem) → 200; PUT /{id} → 200; DELETE /{id} → 204
}
```

- O resource não contém regra nenhuma: recebe os path params, chama o mapper, chama o service e
  devolve o mapper.
- **Filho:** as rotas ficam *dentro do resource do pai*, como `@Path("/{productId}/sizes")` em
  `ProductResource`.
- **Permissão nova:**
  1. adicione ao enum `InternalPermission`;
  2. adicione a constante em `resource/security/Permissions.java`, que tem um teste garantindo que
     os dois batem;
  3. inclua na tabela *Permissions* de `user-management.md`.

  O `super_admin` recebe a permissão nova sozinho no boot.

  Antes de criar uma permissão, confira se uma existente já cobre o caso. Por exemplo, cupons e
  combos se encaixam em `manage_promotions`. Pergunte ao usuário se houver dúvida.

## 12. Teste (`src/test/java/dev/nipponten/CouponRulesTest.java`)

Veja `workflow.md`. Cubra a criação (201), cada regra de negócio (400 e 404), a permissão (403 sem
a role e 401 sem token) e a exclusão bloqueada por dependentes.

## 13. Documentação

| O que mudou | Onde documentar |
| --- | --- |
| Regra de negócio e campos | Doc do domínio (`products-and-promotions.md`, `user-management.md` ou uma doc nova linkada no README) |
| Endpoint e permissão | Tabela *Acesso à API* em `user-management.md` |
| Padrão novo ou exceção a um padrão | `architecture.md` |
| Novo caso de transação no service | Seção *Transação* de `architecture.md` |
| Modelo de dados | Avise que o `docs/data-modeling/data_modeling.drawio` precisa ser atualizado à mão |
