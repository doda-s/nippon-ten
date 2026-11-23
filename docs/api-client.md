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

## Pedidos

As regras estão em [`orders.md`](orders.md). O que muda para o front:

- **O carrinho é do front.** Guarde os itens localmente e chame `POST /orders/quote` sempre que o carrinho mudar, para mostrar preços, promoções e totais. Nunca calcule nem envie preço: a API recalcula tudo na cotação e no checkout.
- **Itens indisponíveis** voltam como `400` no mesmo formato da falha de validação (`{message, violations: [{field, message}]}`), listando todos os itens com problema de uma vez. O `field` aponta o item do carrinho (`items[1]`, `items[1].additionals[0]`), para o front marcar o que o cliente precisa ajustar.
- **Checkout:** `POST /orders` devolve `201` com `{order, trackingToken}`. O mesmo endpoint serve visitante (sem token), cliente logado e funcionário; os campos aceitos mudam com quem chama (ver [[orders#Quem faz o pedido|orders.md]]).
- **Visitante:** `guestEmail` é obrigatório no checkout. `trackingToken` vem uma única vez na resposta, porque o banco guarda só o hash. Guarde o token (ex.: `localStorage`, por pedido) e envie no header `X-Order-Token` nas chamadas daquele pedido.
- **Página do pedido:** o e-mail de confirmação leva o link `<front>/pedido/<code>#t=<trackingToken>` (cliente com conta: sem `#t=`, e a página pede login). A rota `/pedido/:code` do front lê o token do fragmento (`location.hash`), guarda como acima e chama `GET /orders/by-code/{code}` com `X-Order-Token`. Com o `id` da resposta, abre o SSE de `GET /orders/{id}/events` para o status em tempo real. Depois de ler o token, limpe o fragmento da barra de endereço (`history.replaceState`).
- **Perdeu o link:** `POST /orders/tracking-link` com `{code, email}` reenvia o link e responde sempre `202`. O link anterior deixa de funcionar, então o front troca o token guardado quando o visitante abrir o novo. Acima do limite, `429` com `Retry-After`.
- **Tempo real:** `GET /orders/{id}/events` (cliente) e `GET /orders/events` (dashboard) são *Server-Sent Events*. O `EventSource` nativo do navegador não envia headers, então não carrega o `Authorization` nem o `X-Order-Token`. Use um cliente SSE sobre `fetch`, como `@microsoft/fetch-event-source`, com os mesmos headers das outras chamadas. Ao reconectar, recarregue o pedido (`GET /orders/{id}`) ou a listagem: o SSE é só um aviso, e eventos perdidos durante a queda não são reenviados.

## Pagamento online

As regras estão em [`payments.md`](payments.md). O backend usa só o *access token*. O front precisa da **public key** do Mercado Pago (de teste em dev, de produção em produção) para o Card Payment Brick, e só dela.

### Checkout

1. Crie o pedido com `paymentChannel: "ONLINE"` e `paymentMethod` `PIX` ou `CARD` (`CASH` online é `400`). O pedido nasce em `AWAITING_PAYMENT` e a cozinha ainda não o vê.
2. Crie a cobrança com `POST /orders/{id}/payments`, com o `X-Order-Token` do visitante ou logado. A resposta (`201`) é o pagamento: `{id, method, status, statusDetail, amount, pixQrCode, pixQrCodeBase64, expiresAt, approvedAt, createdAt}`.
3. Acompanhe o resultado pelo SSE do pedido (`GET /orders/{id}/events`): na aprovação chegam `PAID` e `STATUS_CHANGED` com o pedido em `PENDING`. Se o SSE cair, recarregue `GET /orders/{id}` e `GET /orders/{id}/payments`.

O valor nunca sai do front: a cobrança é sempre o `total` do pedido. A edição de itens de um pedido online é bloqueada.

### Pix

- O corpo é só `{"method": "PIX"}`. O e-mail do pagador é o `guestEmail` do pedido, ou o da conta do cliente.
- Mostre `pixQrCodeBase64` como imagem (`data:image/png;base64,...`) e `pixQrCode` com um botão de copiar. O Pix vale até `expiresAt` (30 minutos).
- Enquanto o Pix estiver aberto, uma nova cobrança é `400`: reaproveite o QR que já existe. Depois de `expiresAt`, uma nova chamada cria outro Pix.
- Um pedido sem pagamento aberto é cancelado pelo sistema 30 minutos depois de criado (`cancelReason = "Payment not completed"`), e chega o evento `CANCELLED`.

### Cartão

Monte o Card Payment Brick com a public key e o `total` do pedido em `initialization.amount`, sem parcelamento (`customization.paymentMethods.maxInstallments: 1`). No `onSubmit`, repasse ao backend o que o Brick devolve:

```ts
onSubmit: async (brick) => {
  const { data } = await api.post(`/orders/${orderId}/payments`, {
    method: "CARD",
    card: {
      token: brick.token,
      paymentMethodId: brick.payment_method_id,
      payerEmail: brick.payer.email,
      identification: brick.payer.identification, // { type: "CPF", number: "..." }
    },
  });
  // data.status: APPROVED, REJECTED ou PENDING
}
```

- `APPROVED`: o pedido já foi liberado. Mostre a confirmação.
- `REJECTED`: mostre a mensagem do motivo em `statusDetail` (por exemplo `cc_rejected_insufficient_amount`) e deixe o cliente tentar de novo. Cada tentativa precisa de um token novo do Brick, porque o token é de uso único.
- `PENDING`: o cartão ficou em análise. Mostre "pagamento em análise" e espere o SSE.

### Erros e cancelamento

- `502` (`Payment provider unavailable, try again`): o Mercado Pago não respondeu. Tentar de novo é seguro, porque a nova chamada repete a mesma chave de idempotência e não cobra duas vezes.
- `POST /orders/{id}/cancel` num pedido pago online estorna o valor total e cancela. Se o Mercado Pago não aceitar o estorno na hora, o pedido é cancelado mesmo assim e o backend tenta de novo a cada minuto: enquanto `GET /orders/{id}/payments` mostrar o pagamento `APPROVED` num pedido `CANCELLED`, exiba "estorno em processamento".
- O evento `CHARGED_BACK` avisa uma contestação no cartão. O status do pedido não muda.
- O webhook (`POST /payments/webhooks/mercadopago`) é chamado pelo Mercado Pago e não aparece no client.

### Testar no sandbox

Use as credenciais de teste do painel do Mercado Pago (o backend com o *access token* de teste, o front com a *public key* de teste).

- **Pix:** um visitante com `guestName` `APRO` tem o Pix aprovado sozinho pelo sandbox, sem pagar o QR code.
- **Cartão:** use os cartões de teste do Mercado Pago (Mastercard `5480 8328 0103 3311` ou Visa `4235 6477 2802 5682`, CVV `123`, validade `11/30`, CPF `12345678909`). O nome do titular escolhe o resultado: `APRO` aprova, `OTHE` recusa, `CONT` fica em análise, `FUND` recusa por saldo insuficiente.

