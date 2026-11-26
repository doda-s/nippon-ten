> **Status:** implementado. Completa o escopo 13 de [`requirements.md`](requirements.md) com a taxa de entrega, que ficou fora da v1 dos pedidos ([`orders.md`](orders.md)).

A taxa de entrega é calculada pela **distância em linha reta** entre a loja e o endereço do cliente e cobrada em **faixas**: até 3 km, R$5; até 6 km, R$8; e assim por diante. A maior faixa é o raio de entrega. Fora dele, o pedido de entrega é recusado.

O CEP sozinho não basta para isso. Ele dá rua, bairro, cidade e UF, mas a coordenada que algumas APIs de CEP devolvem é o centro da faixa de CEP, não a casa. Por isso o cálculo usa duas consultas:

1. **CEP → endereço**, no ViaCEP, com a BrasilAPI de reserva. São gratuitas e não usam chave. Completam bairro, cidade e UF, e alimentam o autocompletar do front.
2. **Endereço → coordenada**, no Google Geocoding, com rua, número e bairro, filtrando por cidade e UF.

Modelo de dados: página *Delivery* de [`data-modeling/data_modeling.drawio`](data-modeling/data_modeling.drawio).

## Configuração

Existe uma configuração única, em `GET /delivery/settings` e `PUT /delivery/settings`:

| Campo | Significado | Regra |
| --- | --- | --- |
| `storeAddress` | Endereço da loja: `streetAddress`, `number`, `cep` e `complement` | Obrigatório. Rua, número e CEP como no endereço do cliente |
| `pinnedLocation` | Pino posto à mão no mapa (`latitude`, `longitude`). Se vier, a distância é medida dele | Opcional. Os dois campos juntos, entre -90 e 90, e entre -180 e 180 |
| `tiers[].maxDistanceKm` | Fim da faixa, em km | `> 0`, até 2 casas, sem repetir |
| `tiers[].fee` | Taxa da faixa, em R$ | `≥ 0`, 2 casas |

`tiers` tem de 1 a 20 faixas, em qualquer ordem: a API guarda e devolve em ordem crescente. Uma distância cai na primeira faixa com `distância ≤ maxDistanceKm`.

```json
{
  "storeAddress": { "streetAddress": "Rua Exemplo", "number": "100", "cep": "13560-042" },
  "tiers": [
    { "maxDistanceKm": 3, "fee": 5.00 },
    { "maxDistanceKm": 6, "fee": 8.00 }
  ]
}
```

A migration cria a configuração **sem a loja**, com `enabled = false`. Enquanto ninguém configurar, a taxa continua `0`, como na v1, e nada é geocodificado. Não existe rota para desligar o cálculo depois de configurado.

A leitura é pública, para o front mostrar as faixas e o raio. A alteração exige `manage_delivery` e vale **só para pedidos novos**: o pedido guarda a taxa e a distância do próprio checkout (`orders.delivery_fee` e `orders.delivery_distance_km`).

### Posição da loja

Digitar latitude e longitude era difícil para quem configura a loja. Por isso a loja é cadastrada pelo **endereço**, e o pino no mapa serve para corrigir:

1. O admin digita o CEP, a rua e o número. O front completa a rua pela consulta de CEP.
2. `POST /delivery/settings/geocode` recebe o mesmo `storeAddress` e devolve o endereço completo (`address`, com bairro, cidade e UF) e a coordenada achada (`location`). Ele não grava nada. O front põe o pino nessa coordenada.
3. Se o pino estiver no lugar certo, o `PUT` vai só com o endereço. Se o admin arrastar o pino, o `PUT` leva também `pinnedLocation`.

No `PUT`, o endereço é completado pelo CEP e geocodificado como o do cliente (ver [[#Google Geocoding|Google Geocoding]]):

| Caso | Resposta |
| --- | --- |
| CEP inexistente | `400` (`cep 99999-999 was not found`) |
| Sem pino, e o Google não achou o endereço com precisão de rua | `400` (`The store address could not be located: check the street, number and cep, or place the pin on the map`) |
| Provedor de CEP ou Google fora do ar | `502` (`Address lookup unavailable, try again`) |

Com o pino, o endereço não precisa ser achado pelo Google: é para isso que o pino existe. No `geocode`, o endereço não achado não é erro: a resposta vem com `location` nulo, e o front pede o pino.

**A coordenada do endereço não é gravada na configuração.** Os termos do Google só permitem guardar a coordenada por 30 dias. Ela fica no cache `geocoded_address`, como a dos clientes, e é pedida de novo ao Google quando vence, cerca de uma vez por mês. O pino é diferente: a coordenada foi escolhida pelo admin, e por isso fica gravada em `store_latitude` e `store_longitude`.

A resposta de `GET` e `PUT` traz:

- `enabled`;
- `storeAddress`, com bairro, cidade e UF;
- `storeLocation` (`latitude`, `longitude`), de onde a distância é medida;
- `storeLocationPinned`, que diz se a posição é o pino (`true`) ou a coordenada do endereço (`false`);
- `tiers` e `maxDistanceKm`, que é o raio de entrega (a maior faixa).

Se o Google estiver fora do ar e o cache vencido, `storeLocation` sai nulo na leitura, mas a configuração continua ligada.

A configuração feita antes do endereço existir tem só as coordenadas. Ela continua valendo como pino, com `storeAddress` nulo, até alguém salvar de novo.

O mapa do painel admin usa o Google Maps JavaScript API. Os termos do Google não permitem mostrar um resultado do Geocoding num mapa que não seja do Google.

## Cálculo

Em toda cotação e todo checkout de `DELIVERY` com o cálculo ligado:

1. O endereço é completado com bairro, cidade e UF do CEP (ver [[#Consulta de CEP|Consulta de CEP]]).
2. O `GeocodingService` busca a coordenada do endereço e, sem pino, também a da loja: primeiro no cache e, se não estiver lá, no Google.
3. O `DeliveryFeeService` calcula a distância (fórmula de Haversine, arredondada para 2 casas) e escolhe a faixa.

**O backend sempre recalcula a taxa**, como faz com os preços. O front nunca envia coordenada nem taxa.

A entrega é recusada com `400` quando não dá para calcular. Nesse caso o cliente ainda pode escolher `PICKUP`:

| Caso | `message` |
| --- | --- |
| CEP inexistente | `cep 99999-999 was not found` |
| O Google não achou o endereço, achou só o centro do bairro ou da cidade, ou achou em outra cidade ou UF | `The address could not be located for delivery: check the street, number and cep` |
| O Google falhou, recusou a chave ou estourou a cota (ou a chave não está configurada), ao buscar o endereço do cliente ou o da loja | `The delivery fee could not be calculated now: try again or choose PICKUP` |
| Distância maior que a maior faixa | `The address is 22.25 km away and the delivery reaches 6.00 km` |

A recusa vale também quando o Google está fora do ar. Por decisão de produto, a loja prefere perder a venda de entrega a cobrar uma taxa errada.

### Na cotação

`POST /orders/quote` aceita `fulfillmentType`, `addressId` e `address`. A resposta ganha `deliveryFee` e `deliveryDistanceKm`, e o `total` passa a incluir a taxa.

- **Sem `fulfillmentType`, ou com `PICKUP` ou `DINE_IN`:** a taxa é `0`. Mandar um endereço nesse caso é `400`.
- **`DELIVERY` sem endereço:** a cotação mostra só os itens, com taxa `0`, e o checkout continua exigindo o endereço. Assim o carrinho funciona antes de o cliente escolher onde receber.
- **`addressId`:** exige login de cliente, e o endereço tem de ser dele (`404` se não for).
- **`address` digitado:** vale para qualquer um, inclusive o cliente logado que quer ver a taxa de um endereço novo.

### No checkout e na edição

`POST /orders` calcula a taxa do mesmo jeito e grava no pedido a taxa, a distância e o endereço completo, com bairro, cidade e UF. As regras de quem manda `addressId` ou `address` continuam as de [[orders#Quem faz o pedido|orders.md]].

A edição de itens (`PUT /orders/{id}/items`) mantém a taxa e a distância do checkout, porque o endereço não muda.

### Pontos

A taxa de entrega não rende pontos e não pode ser paga com eles. O desconto em pontos continua limitado aos itens elegíveis, e o crédito na conclusão já usava `total − deliveryFee` (ver [`points.md`](points.md)). A estimativa `pointsToEarn` da cotação também desconta a taxa.

## Consulta de CEP

`GET /addresses/cep/{cep}` é pública e aceita o CEP com ou sem hífen (`01310-100` ou `01310100`). Ela devolve `{cep, street, neighborhood, city, state}`. `street` e `neighborhood` são nulos no CEP único de cidade pequena, e nesse caso o front pede a rua ao cliente.

| Situação | Resposta |
| --- | --- |
| Formato inválido | `400` (`must be a CEP like 01310-100`) |
| Nenhum provedor conhece o CEP | `404` |
| ViaCEP e BrasilAPI fora do ar, e o CEP fora do cache | `502` (`Address lookup unavailable, try again`) |

A consulta tenta o ViaCEP primeiro. Se ele falhar ou não conhecer o CEP, tenta a BrasilAPI, porque as bases divergem um pouco e um CEP novo pode existir só em uma delas. O resultado fica em cache (`cep_address`) por 90 dias. Vencido o prazo, o CEP é consultado de novo, e se os dois provedores estiverem fora do ar vale a cópia antiga.

O cadastro e a edição de um *User Address* consultam o CEP e gravam bairro, cidade e UF (CEP inexistente é `400`). Esses campos não vêm no corpo, porque são eles que vão para a geocodificação. Os endereços salvos antes desta mudança ficam sem eles, e o pedido de entrega consulta o CEP na hora. O CEP de `UserAddressRequest` e de `OrderAddressRequest` passa a ter formato validado.

## Google Geocoding

- **Busca.** O texto vai com rua, número e bairro, e o filtro `components` vai com `country:BR|administrative_area:<UF>|locality:<cidade>`. O CEP fica de fora: a base de CEPs do Google no Brasil é incompleta, e com o CEP no texto o Google chegou a devolver uma rua de mesmo nome em outra cidade.
- **Precisão.** Só valem os resultados `ROOFTOP`, `RANGE_INTERPOLATED` e `GEOMETRIC_CENTER`, e só na cidade e UF do CEP. `APPROXIMATE` é o centro do bairro ou da cidade, longe demais para cobrar por km.
- **Cache de 29 dias.** Os termos do Google só permitem guardar latitude e longitude por 30 dias seguidos. O `geocoded_address` guarda a coordenada com chave no SHA-256 do endereço normalizado, sem o endereço em claro. O `GeocodeCachePurgeJob` apaga as linhas vencidas todo dia às 4h. No Cloud Run, onde o scheduler interno fica desligado, a mesma limpeza é disparada pelo Cloud Scheduler em `POST /jobs/geocode-cache-purge` (ver *Jobs agendados* em [`deploy.md`](deploy.md)). O "não achou" também fica em cache, para a cotação repetida não chamar o Google de novo.
- **Custo.** O SKU *Geocoding* (Essentials) tem 10.000 chamadas grátis por mês, somadas em todos os projetos da conta de faturamento. Depois disso, custa US$5 a cada 1.000. Como a cotação e o checkout do mesmo endereço fazem uma chamada só, o volume de uma pastelaria fica na faixa grátis.
- **Chave.** `GOOGLE_MAPS_API_KEY`, restrita à Geocoding API. No staging ela está no secret `google-maps-api-key`, com cota de 300 chamadas por dia no projeto, para um erro ou abuso não gerar conta. Sem a chave a aplicação sobe, mas a entrega com o cálculo ligado responde `400`.

```properties
quarkus.rest-client.viacep.url=${VIACEP_BASE_URL:https://viacep.com.br}
quarkus.rest-client.brasilapi.url=${BRASILAPI_BASE_URL:https://brasilapi.com.br}
quarkus.rest-client.google-geocoding.url=${GOOGLE_GEOCODING_BASE_URL:https://maps.googleapis.com}
nippon.delivery.google.api-key=${GOOGLE_MAPS_API_KEY:}
nippon.delivery.cep-cache-ttl=P90D
nippon.delivery.geocode-cache-ttl=P29D
nippon.delivery.geocode-cache-purge-cron=0 0 4 * * ?
```

Em teste, os três provedores são o `GeoStub` (`src/test`), um servidor HTTP local em que a rua e o CEP escolhem a resposta.

## Riscos conhecidos

- **Linha reta não é rota.** Um rio ou uma rodovia pode dobrar o caminho real. Se isso pesar, a Routes API do Google (Compute Route Matrix) dá a distância de carro, também com cobrança por chamada.
- **Cota esgotada.** A cotação é pública. Alguém que cote muitos endereços diferentes gasta a cota diária de 300 chamadas e derruba o cálculo da entrega até o dia seguinte, quando as entregas novas passam a ser recusadas. O cache segura os endereços já vistos. Se acontecer, suba a cota ou ponha um limite por IP, que depende do IP real do cliente atrás do proxy (ver [[architecture#Proxy ou load balancer na frente da API|architecture.md]]).
- **Consulta de CEP sem limite.** O ViaCEP e a BrasilAPI são gratuitos, mas podem bloquear o IP da API se ela consultar CEPs demais. O cache de 90 dias reduz as chamadas. Pelo mesmo motivo do item anterior, não existe limite por IP.
- **Endereço errado de propósito.** O cliente pode informar um endereço mais perto para pagar menos. A entrega vai para o endereço do pedido, e o funcionário vê o endereço e a distância gravados.

## Acesso à API

| Rota | Acesso |
| --- | --- |
| `GET /delivery/settings` | pública |
| `PUT /delivery/settings` | `manage_delivery` |
| `POST /delivery/settings/geocode` | `manage_delivery` (gasta a cota do Google) |
| `GET /addresses/cep/{cep}` | pública |
| `POST /jobs/geocode-cache-purge` | pública, validada pelo header `X-Job-Token` (Cloud Scheduler) |

## Modelo de dados

```
delivery_settings(id = 1, store_street_address NULL, store_number NULL, store_complement NULL, store_cep NULL,
                  store_neighborhood NULL, store_city NULL, store_state NULL,
                  store_latitude NULL, store_longitude NULL, updated_at, updated_by_user_id NULL)
delivery_fee_tier(delivery_settings_id, max_distance_km, fee)  -- PK (delivery_settings_id, max_distance_km)
cep_address(cep PK, street NULL, neighborhood NULL, city, state, fetched_at)
geocoded_address(address_hash PK, latitude NULL, longitude NULL, geocoded_at)
user_address + neighborhood NULL, city NULL, state NULL
orders       + neighborhood NULL, city NULL, state NULL, delivery_distance_km NULL
```

- O endereço da loja vem inteiro ou não vem (rua, número, CEP, cidade e UF). `store_latitude` e `store_longitude` são o pino, e também vêm juntos.
- As coordenadas são `double precision`. `max_distance_km` é `numeric(5, 2)`, `delivery_distance_km` é `numeric(6, 2)`, e `fee` é `numeric(38, 2)`, como os outros valores em dinheiro.
- As faixas são valores da configuração, sem identidade própria: cada `PUT` as substitui inteiras.
- `cep_address` e `geocoded_address` são caches. Não têm FK e podem ser apagados a qualquer momento.
- `delivery_distance_km` é nulo nos pedidos sem entrega, nos de antes desta mudança e nos feitos com o cálculo desligado.
