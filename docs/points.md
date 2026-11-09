> **Status:** implementado. Cobre o escopo 8 e os requisitos 36 e 37 de [`requirements.md`](requirements.md).

O programa de pontos devolve ao cliente parte do que ele gasta. O cliente com conta ganha pontos quando um pedido é concluído e usa esses pontos como desconto em pedidos futuros. O visitante (*Guest User*) não acumula nem usa pontos (ver [[user-management#Guest User|user-management.md]]).

Modelo de dados: página *Points* de [`data-modeling/data_modeling.drawio`](data-modeling/data_modeling.drawio).

## Configuração

Existe uma configuração única para todo o sistema (req. 36), em `GET /points/settings` e `PUT /points/settings`:

| Campo | Significado | Regra |
| --- | --- | --- |
| `earnRate` | Pontos ganhos por R$1 pago | `≥ 0`, até 4 casas. `0` desliga o acúmulo. |
| `pointValue` | Quanto um ponto vale em R$ no resgate | `> 0`, 2 casas |
| `expirationDays` | Por quantos dias um ponto ganho vale | `> 0`, ou ausente para os pontos não expirarem |

A migration cria a configuração com `earnRate = 1`, `pointValue = 0.05` e `expirationDays = 365`, ou seja: R$1 pago rende 1 ponto, e 20 pontos valem R$1.

A leitura é pública, para o front mostrar quanto vale um ponto e quantos pontos o pedido rende. A alteração exige `manage_points` e vale **só para pedidos novos**: no checkout, o pedido guarda uma cópia de `earnRate` e `pointValue` (`orders.earn_rate` e `orders.point_value`). O pedido de um visitante não guarda essas cópias.

## Acúmulo

Quando o pedido de um cliente passa para `COMPLETED`, ele rende:

```
pontos = piso((total − deliveryFee) × earnRate do checkout)
```

A base é o que o cliente pagou em dinheiro, já sem o desconto de pontos. A taxa de entrega fica fora dela. O pedido grava os pontos em `pointsEarned`, e o extrato ganha uma linha `EARN` que vence em `expirationDays`, contados a partir da conclusão. A validade usada é a da configuração atual.

Os pontos só entram na conclusão. Por isso um pedido cancelado nunca rende pontos, e não existe estorno de ganho (`COMPLETED` é final).

## Resgate

O cliente informa `pointsToRedeem` no checkout (`POST /orders`). O mesmo campo existe na cotação (`POST /orders/quote`), para o carrinho mostrar o desconto antes do checkout.

- O desconto é `pointsToRedeem × pointValue`. Ele vai para o campo `discount`, e `total = subtotal − discount + deliveryFee`.
- O resgate pode ser parcial: o cliente usa quantos pontos quiser, dentro dos limites abaixo.
- **Base elegível.** O desconto não passa da soma dos itens elegíveis (`pointsEligibleSubtotal` na cotação). Um item de produto em promoção com `enablePromotionPoints = false` não é elegível (ver [[products-and-promotions#Promotions|products-and-promotions.md]]). Itens de combo e itens sem promoção são elegíveis.
- Se o desconto passar da base elegível, a resposta é `400` com o máximo de pontos aceito. A API recusa em vez de cortar o valor, para o cliente nunca gastar um ponto que não vira desconto.
- Saldo insuficiente é `400`.
- Só vale para pedido de cliente: o cliente logado, ou o funcionário que lança o pedido com `clientId`. Para um visitante, `pointsToRedeem` é `400`.
- Na cotação, `pointsToRedeem` exige login de cliente (`400` sem ele), porque a cotação confere o saldo. A cotação também devolve `pointsToEarn`, uma estimativa de quantos pontos o pedido renderia com a taxa atual.

Os pontos saem do saldo no checkout, junto com a criação do pedido. O pedido guarda a quantidade em `pointsRedeemed`.

**Cancelamento.** Quando um pedido com pontos resgatados é cancelado, os pontos voltam para o cliente (linha `REFUND`) e mantêm o vencimento original. Um ponto que venceu enquanto o pedido estava aberto expira logo depois do estorno.

**Edição pelo funcionário.** `PUT /orders/{id}/items` mantém os pontos resgatados e as taxas do checkout. Se a nova base elegível ficar menor que o desconto, a resposta é `400`. Nesse caso, o funcionário cancela o pedido (o que devolve os pontos) e faz outro.

## Saldo, lotes e expiração

Cada crédito (`EARN` ou `ADJUST` positivo) é um **lote**, com o saldo que ainda pode ser gasto e uma data de vencimento. Um gasto consome primeiro os lotes que vencem antes; os lotes sem vencimento ficam por último. Cada lote consumido gera uma linha própria no extrato, e é por isso que o estorno sabe para onde devolver cada ponto.

A expiração não depende de agendador. Antes de ler ou mudar o saldo de um cliente, o `PointsService` zera os lotes vencidos e grava uma linha `EXPIRE` para cada um. Um lote vencido nunca conta como saldo, mesmo antes de a linha `EXPIRE` existir. "Agora" vem do `Clock` injetado.

O extrato (`points_transaction`) é a fonte da verdade. `client.promotion_points` guarda o saldo em cache e é regravado na mesma transação de cada movimento. Antes de mexer no saldo, o service trava a linha do cliente (`select … for update`), para que dois checkouts simultâneos não gastem o mesmo ponto.

## Extrato

- `GET /users/{id}/points` devolve `{balance, nextExpiration}`. `nextExpiration` é o próximo lote a vencer (`{points, expiresAt}`), ou `null` se nenhum ponto do saldo vence.
- `GET /users/{id}/points/transactions?page=&size=` devolve uma página do extrato, da linha mais recente para a mais antiga. Cada linha tem `{id, type, points, orderId, expiresAt, reason, createdAt}`, e `points` tem sinal (negativo para gastos e expirações).

| `type` | Quando |
| --- | --- |
| `EARN` | Pedido concluído |
| `REDEEM` | Pontos usados num pedido (uma linha por lote consumido) |
| `REFUND` | Pedido com pontos cancelado |
| `EXPIRE` | Lote vencido |
| `ADJUST` | Ajuste manual do funcionário |

O acesso segue a regra de `/users/{id}/**`: o próprio usuário ou `manage_users`, conferido no service antes de buscar o cliente. Um usuário interno não tem pontos: recebe saldo `0` e uma página vazia.

## Ajuste manual

`POST /users/{id}/points/adjustments` recebe `{points, reason}` e exige `manage_points`. Serve para corrigir um atendimento, por exemplo.

- `points` positivo cria um lote com a validade da configuração atual.
- `points` negativo gasta como um resgate e não deixa o saldo negativo (`400`).
- `points = 0` é `400`, e `reason` é obrigatório.
- A resposta (`201`) é o saldo atualizado. O extrato guarda quem fez o ajuste.

## Simulação de impacto

`POST /points/settings/simulation` (req. 37) recebe `{earnRate, pointValue, from, to, amount?, points?}` e não grava nada. `from` e `to` são dias de negócio, inclusivos, no fuso do restaurante (`BusinessTime`). `to` antes de `from` é `400`.

A simulação considera os pedidos `COMPLETED` de clientes, criados no período, e compara a configuração atual (`current`) com a proposta (`proposed`):

| Campo | Significado |
| --- | --- |
| `orders`, `revenue` | Quantidade de pedidos e o valor pago em dinheiro (sem taxa de entrega) |
| `pointsEarned` | Pontos que a taxa geraria, arredondados por pedido, como no crédito real |
| `cost` | `pointsEarned × pointValue`: quanto esses pontos valem em desconto |
| `costShareOfRevenue` | `cost` como percentual de `revenue` |
| `outstandingLiability` | Saldo em aberto de todos os clientes hoje × `pointValue` |
| `pointsRedeemed` | Pontos usados em pedidos não cancelados do período |
| `outstandingPoints` | Saldo em aberto (não vencido) de todos os clientes hoje |
| `amount`, `points` | Ecoam a compra e os pontos de exemplo enviados (`null` se ausentes) |
| `pointsForAmount` | Pontos que uma compra de `amount` renderia com o `earnRate` do cenário, arredondados para baixo como no crédito real (`null` sem `amount`) |
| `pointsWorth` | `points × pointValue`: quanto `points` pontos valem em desconto no cenário (`null` sem `points`) |

`amount` e `points` são opcionais e independentes do período: servem para testar a conversão dinheiro → pontos e pontos → dinheiro das duas configurações lado a lado. `amount` é o valor pago sem taxa de entrega, a mesma base do crédito.

## Acesso à API

| Rota | Acesso |
| --- | --- |
| `GET /points/settings` | pública |
| `PUT /points/settings`, `POST /points/settings/simulation` | `manage_points` |
| `GET /users/{id}/points`, `GET /users/{id}/points/transactions` (paginado) | o próprio usuário ou `manage_users` |
| `POST /users/{id}/points/adjustments` | `manage_points` |

## Modelo de dados

```
points_settings(id = 1, earn_rate, point_value, expiration_days NULL, updated_at, updated_by_user_id NULL)
points_transaction(id, client_id, order_id NULL, type, points, remaining NULL, expires_at NULL,
                   lot_id NULL, reason NULL, created_by_user_id NULL, created_at)
orders      + points_redeemed, point_value NULL, earn_rate NULL, points_earned
order_item  + points_eligible
client.promotion_points  -- saldo em cache, not null default 0
```

- `remaining` só existe nos lotes (linhas positivas). `lot_id` liga `REDEEM`, `REFUND`, `EXPIRE` e `ADJUST` negativo ao lote afetado.
- `points_transaction.client_id` é `on delete cascade`: quando a conta do cliente é excluída, o extrato vai junto. `order_id` é `on delete set null`.
- `earn_rate` é `numeric(10, 4)`. `point_value` é `numeric(38, 2)`, como os outros valores em dinheiro.
- `created_at`, `updated_at` e `expires_at` são instantes técnicos (`timestamptz`).
