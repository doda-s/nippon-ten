## Camadas

O código está organizado em quatro pacotes sob `dev.nipponten`, de dentro para fora:

- `domain` — `models` (records Java puros, sem anotação de framework) e `repositories` (as *portas*: interfaces que o domínio exige para persistir e recuperar seus dados, sem saber como isso é feito).
- `application` — `services` (orquestração e regras de aplicação), `requests`/`responses` (DTOs de entrada e saída) e `exceptions` (erros de domínio/aplicação).
- `infrastructure` — `entities` (JPA/Panache), `mappers` (Entity ↔ Domain Model), `repositories` (as *implementações* Panache das portas do domínio), `security` (JWT e identidade do request), `bootstrap` (rotinas de startup) e `time` (o `Clock` da aplicação).
- `resource` — os endpoints REST (Quarkus/JAX-RS), as constantes das anotações de segurança (`resource.security`) e o mapeamento de exceção para resposta HTTP (`resource.exception`).

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

## Paginação: todo GET de listagem de um resource top-level é paginado

Todo `GET` que lista um agregado top-level (não os filhos aninhados sob um pai, como `/products/{id}/sizes`) aceita `page` (padrão `0`) e `size` (padrão `20`, teto `100`) como query param — normalizados por `PageParams.of` — e devolve um `PageResponse` (`application.responses`): `{content, page, size, totalElements, totalPages}`.

No `domain.repositories`, o método é `PageResult<X> getPage(int page, int size)` (variantes filtradas, como disponibilidade de combo/promoção, ganham `getXxxPage`); o `Service` só repassa para o repositório. Na implementação Panache, a query usa sempre `Sort.by("id")` — sem isso a ordem entre páginas não é estável (o Postgres não garante ordem sem `ORDER BY`), e um registro pode nunca aparecer em nenhuma página ou aparecer duas vezes.

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

- **filhos do próprio agregado são removidos em cascata**: `Product` → `ProductSize`, `ProductIngredient`, `AdditionalIngredient`; `Combo` → `ComboProduct`; `User` → `UserAddress`, `Client`, `Internal`, `RefreshToken`;
- **referências vindas de outro agregado bloqueiam a exclusão** com `InvalidRequestException` (`400`): `Product` usado por combo ou promoção; `ProductSize` usado por combo; `Ingredient` usado por produto ou adicional; `PromotionType` usado por promoção; `InternalRole` atribuída a usuário interno.

---

## Transação: no repositório por padrão, no service quando cruza agregados

Toda escrita de um único agregado é transacional no nível do repositório Panache (`PanacheXRepository.save`/`remove`). Isso é suficiente para a maioria dos casos.

Quando um `Service` precisa orquestrar **mais de um agregado em sequência** dentro da mesma operação — hoje isso acontece em `UserService.register` (cria `User` + `Client` + `UserAddress` opcional), `UserService.registerInternal` (cria `User` + `Internal`), `UserService.delete`/`deleteInternal` (remove `UserAddress`(s) + `Client` + `Internal` + `RefreshToken`(s) + `User`), `SuperAdminService.bootstrap` (cria ou sincroniza a role reservada + `User` + `Internal`), `RefreshTokenService.rotate` (revoga o token atual + emite o próximo; na detecção de reuso, a revogação em massa é gravada mesmo com o `401`, via `dontRollbackOn`), `ProductService.delete` (remove os filhos do produto + `Product`), `ComboService.register` (cria `Combo` + os `ComboProduct`(s) do fluxo de criação) e `ComboService.delete` (remove `ComboProduct`(s) + `Combo`) — o método do `Service` também leva `@Transactional`, garantindo que a operação inteira seja atômica. Fora desses casos, não se adiciona `@Transactional` no nível de `Service`.

Planejados com os pedidos (ver [`orders.md`](orders.md)): `OrderService.create` (lê produto, tamanho, combo, promoção e cliente e grava `Order` + itens + adicionais + removidos + componentes + histórico), `OrderService.updateItems` (substitui os itens e reprecifica) e as mudanças de status, pagamento e cancelamento (alteram o pedido e gravam `order_status_history` juntos). Os eventos SSE dessas operações só são publicados depois do commit.

---

## Validação: Bean Validation nos requests, exceção de domínio para o resto

Todo `XRequest` é anotado com Bean Validation (`@NotNull`, `@NotBlank`, `@Positive`, `@Email` etc.) e todo método de `Resource` que recebe um request usa `@Valid`. A extensão `quarkus-hibernate-validator` intercepta antes do request chegar ao `Service` e devolve `400` automaticamente se alguma constraint falhar. Esse `ConstraintViolationException` é mapeado por `ConstraintViolationExceptionMapper` (`resource.exception`) para o mesmo formato de erro do resto da API — `{"message": "Invalid request", "violations": [{"field": ..., "message": ...}]}` — em vez do payload padrão (mais verboso, com o caminho completo do método) do Hibernate Validator.

`InvalidRequestException` (mapeada para `400` por `InvalidRequestExceptionMapper`) fica reservada para regras que Bean Validation não consegue expressar — invariantes que dependem de mais de um campo, de outra entidade ou de uma consulta (ex.: "um combo precisa ter no mínimo dois produtos"). Se a regra é sobre a forma de um campo isolado, ela é uma anotação no `Request`, não uma checagem manual no `Service`.

Quando vários itens de uma lista podem falhar ao mesmo tempo e o cliente precisa ver todos (hoje, a disponibilidade dos itens do pedido), o `Service` acumula os problemas e lança uma única `UnavailableItemsException`. O `UnavailableItemsExceptionMapper` a devolve como `400` no formato `ValidationErrorResponse`, com o caminho do item em `field` (ex.: `items[1].additionals[0]`), em vez de parar no primeiro erro.

`XNotFoundException` (todas estendendo `NotFoundException`, mapeada para `404` por `GlobalExceptionMapper`) é usada para "esse id não existe" ou "esse filho não pertence a esse pai" — nunca para validação de formato de request.

---

## Autenticação e autorização: identidade no request, regra no Service

O fluxo para os clientes da API (tokens, cookie, refresh) está em [`api-client.md`](api-client.md); quem pode chamar cada endpoint e as regras de permissão estão em [`user-management.md`](user-management.md). Esta seção mostra onde cada parte vive no código.

1. **Token.** `POST /auth/login` emite um access token JWT RS256 de 15 min (`SmallRyeTokenService`) que carrega só identidade: `sub` (id do usuário), `upn` (email), `type` e `sid` (a sessão). Nenhuma permissão vai no token. O refresh token é opaco, guardado só como SHA-256 em `refresh_token`, e muda a cada uso (`RefreshTokenService`).
2. **Identidade do request.** O smallrye-jwt valida assinatura e expiração antes do JAX-RS. Em seguida o `PermissionAugmentor` consulta o banco numa só query (`UserAccessRepository`): usuário ativo, sessão ainda com refresh token válido, role e permissões. Com isso monta a identidade com os grupos `CLIENT`/`INTERNAL`, `SUPER_ADMIN` e uma role por permissão efetiva. O resultado fica 30 s no `AuthenticationCache`, que é invalidado por qualquer mudança de acesso. Por isso desativar um usuário, trocar a senha ou fazer logout valem no request seguinte, sem esperar o token expirar.
3. **Regra que depende só da permissão** fica no `Resource`, como anotação: `@PermitAll`, `@Authenticated` ou `@RolesAllowed(Permissions.X)`. Endpoint sem anotação é negado (`deny-unannotated-endpoints`). As anotações ficam nos métodos, e não na classe, para o OpenAPI marcar só as rotas protegidas.
4. **Regra que depende de dados** fica no `Service`, via `CurrentUser` (porta em `application`, implementada sobre a `SecurityIdentity` em `infrastructure`): `requireSelfOr(permissão, userId)` para "o próprio usuário ou X" e `requireHolds(permissões)` para "ninguém concede o que não tem". Pelo mesmo motivo das demais regras de negócio, elas nunca ficam no `Resource`.

Respostas de erro, todas no formato `ErrorResponse`:

| Status | Origem |
| --- | --- |
| `401` | Falha de token antes do JAX-RS → `AuthenticationFailureHandler` (failure handler do Vert.x, porque os `ExceptionMapper`s não alcançam essa fase); endpoint protegido sem token → `MissingCredentialsExceptionMapper`; login ou refresh recusado → `UnauthorizedExceptionMapper`. Falhas de token usam sempre a mesma mensagem, para não revelar o motivo. |
| `403` | Falta de permissão na anotação, ou `ForbiddenException` lançada pelo `Service` → `ForbiddenExceptionMapper`. |
| `429` | Limite de tentativas de login (`LoginAttemptLimiter`) → `TooManyRequestsExceptionMapper`, com `Retry-After`. |

---

## Datas: instante em UTC, horário de negócio no fuso do restaurante

O sistema guarda dois tipos de data, com tipos diferentes:

| Tipo | Exemplos | Java | Postgres |
|---|---|---|---|
| Instante técnico: quando algo aconteceu ou expira | `user.created_at`, datas de `refresh_token` | `Instant` (UTC) | `timestamptz` |
| Horário de negócio: hora local do restaurante | início e fim de combos e promoções | `LocalDateTime` | `timestamp` |

"Agora" vem sempre de um `Clock` injetado (`ClockProducer`, em UTC), nunca de `now()` direto. Para horários de negócio, `BusinessTime.now()` converte esse relógio para o fuso `nippon.business-zone` (`America/Sao_Paulo`). Assim o resultado não depende do fuso do servidor, e os testes podem adiantar o tempo com `TestClock`.

---

## Autenticação: implantação e riscos conhecidos

A autenticação guarda estado em memória e depende do IP do cliente. Antes de colocar a API atrás de um proxy ou de subir mais de uma instância, confira os pontos abaixo.

### Proxy ou load balancer na frente da API

O limite de tentativas de login conta falhas por e-mail e por IP (`LoginAttemptLimiter`). O IP vem de `HttpServerRequest.remoteAddress()`. Atrás de um proxy, esse endereço é o do proxy, então **todos os clientes passam a dividir o mesmo contador por IP**: 20 falhas de quaisquer usuários bloqueiam o login de todo mundo por 15 minutos.

Para o IP real chegar à API, habilite o encaminhamento no Quarkus e aceite os cabeçalhos **só do proxy confiável**:

```properties
%prod.quarkus.http.proxy.proxy-address-forwarding=true
%prod.quarkus.http.proxy.allow-x-forwarded=true
%prod.quarkus.http.proxy.enable-forwarded-host=true
# Só confia nos cabeçalhos vindos destes endereços (o proxy):
%prod.quarkus.http.proxy.trusted-proxies=10.0.0.10
```

Sem `trusted-proxies`, qualquer cliente pode mandar `X-Forwarded-For` com um IP inventado a cada tentativa e escapar do limite por IP. O limite por e-mail continua valendo de qualquer forma.

### Mais de uma instância da API

Dois componentes guardam estado na memória de cada instância:

| Componente | O que guarda | Efeito com várias instâncias |
|---|---|---|
| `AuthenticationCache` | Resultado da checagem de sessão e permissões, por 30 s (`nippon.auth.session-cache-ttl`) | A invalidação só acontece na instância que recebeu a mudança. Nas outras, logout, desativação e mudança de permissão levam **até 30 s** para valer. |
| `LoginAttemptLimiter` | Falhas de login por e-mail e por IP, em janelas de 15 min | Cada instância conta separado. Com N instâncias e balanceamento sem afinidade, um atacante tem até N vezes mais tentativas. |

Quando os pedidos forem implementados, um terceiro componente entra nesta lista: o broadcaster de eventos SSE dos pedidos ([[orders#Notificações (SSE)|orders.md]]), que também vive em memória. Com várias instâncias, quem está conectado numa instância não recebe os eventos gerados em outra. Como o SSE é só um aviso e o front recarrega o pedido ao reconectar, nada se perde no banco, mas o aviso em tempo real falha. A solução é distribuir os eventos por Postgres `LISTEN/NOTIFY` ou Redis.

O resto do estado (refresh tokens e sessões) fica no banco e é compartilhado. Se o atraso de 30 s não for aceitável, reduza `nippon.auth.session-cache-ttl` (`PT0S` desliga o cache) ou troque os dois componentes por um armazenamento compartilhado, como Redis.

### Riscos conhecidos

- **Refresh concorrente.** Cada refresh token vale para um único uso. Se duas abas chamarem `/auth/refresh` ao mesmo tempo com o mesmo cookie, a segunda é tratada como reuso de token roubado, e todas as sessões do usuário são revogadas. O front deve serializar o refresh (uma chamada por vez, compartilhada entre as abas). Outra opção seria aceitar o token anterior por alguns segundos após a rotação.
- **Sem recuperação de senha.** Fora de escopo por decisão. Um usuário que perde a senha depende de alguém com `MANAGE_USERS` para trocá-la (no caso de um super_admin, de outro super_admin).

---

## O que ainda não existe (fora do escopo desta documentação)

A autenticação e a autorização descritas em [`requirements.md`](requirements.md) e [`user-management.md`](user-management.md) estão implementadas (ver as seções acima). Continua fora, por decisão, a recuperação de senha (requisito 12), que depende de envio de e-mail.

Os pedidos estão especificados em [`orders.md`](orders.md) e são entregues por fases (o roadmap está lá). Por enquanto só a cotação existe (`OrderQuoteService`, `POST /orders/quote`); ela lê o catálogo e não grava nada.

Os domain models são apenas dados (records sem invariantes): as regras de negócio vivem no `Service` do agregado responsável, seguindo os padrões descritos acima. É lá que estão, por exemplo, o mínimo de dois produtos por combo, a promoção única por produto e a validação das janelas de disponibilidade.

A propagação de status "esgotado" entre ingrediente → produto → combo/promoção continua sendo **manual por decisão de produto**, não uma lacuna: `GET /ingredients/{id}/dependents` e `GET /products/{id}/dependents` devolvem quem depende do item para o usuário interno decidir, e o sistema nunca altera esses status sozinho.
