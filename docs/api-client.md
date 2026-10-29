O client Node da API é gerado a partir do schema OpenAPI (`target/openapi/openapi.yaml`, gravado pelo Quarkus no build) e publicado no npm como o pacote `nippon-ten`.

## Instalação

O client é publicado como versão *snapshot* (ex.: `0.1.0-SNAPSHOT.202610011825`), na tag `snapshot` do npm. Para instalar:

```shell
npm install nippon-ten@snapshot
```

## Publicação

Os scripts `scripts/build-client.sh` (Linux/macOS) e `scripts/build-client.bat` (Windows) geram o client com o OpenAPI Generator (`typescript-axios`) em `target/openapi-clients/node-client`, instalam as dependências e publicam o pacote com `npm publish --access public --tag snapshot`.

## Autenticação

A API usa dois tokens:

| Token | Onde fica | Validade | Para quê |
| --- | --- | --- | --- |
| Access token (JWT) | Corpo da resposta de login e refresh; o front guarda **só em memória** | 15 min | Header `Authorization: Bearer` em cada chamada protegida |
| Refresh token | Cookie `nt_refresh` (`HttpOnly; Secure; SameSite=None; Path=/auth`), que o JavaScript não lê | 7 dias | Obter um novo access token em `POST /auth/refresh` |

Front e API ficam em sites diferentes, então:

- toda chamada precisa de `withCredentials: true`, inclusive o login: sem isso o navegador não grava nem envia o cookie;
- `POST /auth/refresh` e `POST /auth/logout` exigem o header `X-Requested-With: nippon-ten` (proteção contra CSRF); sem ele a resposta é `403`;
- a origem do front precisa estar em `FRONT_ORIGINS`, na configuração de CORS da API.

### Configuração do client

```ts
import { AuthResourceApi, Configuration, ProductResourceApi } from "nippon-ten";

let accessToken: string | undefined;

const configuration = new Configuration({
  basePath: "https://api.exemplo.com",
  accessToken: () => accessToken ?? "",
  baseOptions: { withCredentials: true },
});

const auth = new AuthResourceApi(configuration);
const products = new ProductResourceApi(configuration);
```

### Fluxo

```ts
// Login: o access token vem no corpo; o refresh token, no cookie.
const { data } = await auth.authLoginPost({ email, password });
accessToken = data.accessToken;

// Ao abrir ou recarregar a página, e ao receber 401: renova a sessão pelo cookie.
// O segundo parâmetro (o cookie) fica vazio: quem envia é o navegador.
const refreshed = await auth.authRefreshPost("nippon-ten");
accessToken = refreshed.data.accessToken;

// Dados do usuário logado: tipo, nome, superAdmin e permissões efetivas.
const me = await auth.authMeGet();

// Logout: revoga a sessão e apaga o cookie. O access token dela para de valer na hora.
await auth.authLogoutPost("nippon-ten");
accessToken = undefined;
```

**Faça um refresh por vez.** Cada refresh token vale para um único uso. Se duas chamadas a `/auth/refresh` saírem com o mesmo cookie, a segunda é tratada como token roubado, e todas as sessões do usuário são encerradas. Compartilhe uma única promessa de refresh dentro da aba e, entre abas, serialize com `navigator.locks.request("nt-refresh", ...)`.

### Respostas de erro

Todas usam o corpo `{ "message": "..." }`.

| Status | Quando | O que o front faz |
| --- | --- | --- |
| `401` | Sem token, token inválido ou expirado, sessão encerrada (logout, troca de senha, usuário desativado) | Tenta um refresh; se ele também der `401`, manda para o login |
| `403` | Logado, mas sem a permissão, ou tentando acessar a conta de outro usuário | Mostra "sem permissão"; refresh não resolve |
| `429` | Muitas tentativas de login erradas (5 por e-mail ou 20 por IP em 15 min) | Mostra a espera; o header `Retry-After` traz os segundos |

O login devolve sempre a mesma mensagem (`Invalid email or password`) para senha errada, e-mail inexistente ou usuário desativado.
