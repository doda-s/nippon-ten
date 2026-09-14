## Product

Um *Product* representa um item/serviço oferecido pelo cliente do sistema. O *Product* é composto pelas seguintes informações:

- Name: É o nome que será exibido para os usuários que consomem os produtos/serviços do cliente do sistema.
- Product Size: É a configuração que define qual o tamanho do produto.
- Image URL: URL da imagem ilustrativa do produto.
- Description: Descrição com os detalhes do produto.
- Status: Status representa a disponibilidade do produto (disponível ou esgotado). O status pode ser alterado manualmente por um usuário interno.

O produto pode ter seu status alterado caso um ingrediente se esgote. Essa propagação **não é automática**: ao marcar um ingrediente (ou produto) como esgotado, o usuário interno recebe a lista de produtos/combos/promoções que dependem dele e escolhe, manualmente, quais também devem ser marcados como esgotados naquele momento. O sistema nunca decide isso sozinho.

Para a criação de um produto, o usuário interno precisa conter a [[user-management#Permissions|permissão]] `manage_products` para poder criar, editar, deletar e definir o status um produto.

### Product Ingredients

*Product Ingredients* representa a relação entre os ingredientes cadastrados, e os ingredientes necessários para o produto. Essa relação é feita durante a criação de um produto. Nesse fluxo o usuário interno define quais são os ingredientes que o produto depende.

### Additional Ingredients

*Additional Ingredients* representa um ingrediente **opcional** que o cliente pode adicionar ao pedido daquele produto (diferente de *Product Ingredient*, que é a composição obrigatória do produto). Cada *Additional Ingredient* define, para um produto e um ingrediente específicos, a quantidade máxima (`maximum_quantity`) que o cliente pode adicionar naquele item do pedido, e um status de disponibilidade. É essa configuração que permite ao sistema limitar a quantidade de adicionais no pedido (ver `requirements.md`).

### Product Size

O *Product Size* representa os tamanhos disponíveis para um produto. Os tamanhos (ex.: Pequeno, Médio, Grande) formam um catálogo **global**, reaproveitável entre produtos — um usuário interno cadastra o tamanho uma vez e ele fica disponível para ser associado a qualquer produto. A relação entre um produto e um tamanho é feita por uma tabela auxiliar própria, que também guarda o **preço daquela combinação específica de produto+tamanho** (o mesmo tamanho pode ter preços diferentes em produtos diferentes) e o status de disponibilidade daquela combinação. Para poder configurar tamanhos (o catálogo global) ou associá-los a um produto, o usuário interno precisa conter a [[user-management#Permissions|permissão]] `manage_products`.

> Modelo de dados: `size(id, name)` como catálogo global; `product_size(id, product_id, size_id, price, status)` como tabela auxiliar de associação. O diagrama em `docs/data-modeling/data_modeling.drawio` ainda mostra `product_size` sem a tabela `size` e sem o campo `name` — precisa ser atualizado manualmente para refletir esse desenho.

---
## Ingredient

*Ingredient* representa a matéria prima para os produtos serem confeccionados. Os dados que compõem um *ingredient* são:

- Name: É o nome para identificação do ingrediente.
- Description (opcional): Uma descrição detalhada do ingrediente.
- Image URL: URL da imagem de ilustração do ingrediente.
- Price: É o preço usado para calcular o acréscimo ao adicionar um ingrediente como adicional em um pedido
- Status: Status representa a disponibilidade do produto (disponível ou esgotado). O status pode ser alterado manualmente por um usuário interno.

Quando um ingrediente é marcado como esgotado, o sistema lista os produtos que dependem dele e o usuário interno escolhe manualmente quais também devem ser marcados como esgotados — não há propagação automática (mesma regra descrita na seção [[#Product|Product]]).

Para um ingrediente ser cadastrado, editado, deletado ou marcado como esgotado, o usuário interno precisa conter a [[user-management#Permissions|permissão]] `manage_ingredients`.

---
## Combos

Os combos são promoções que contém múltiplos produtos, que são vendidos por um preço específico. Os combos podem, ou não, ter um período de duração. Os dados que compõem um combo são:

- Name: Nome de exibição do combo;
- Price: O valor cobrado pelo combo;
- Image URL: URL da imagem de ilustração do combo;
- Description: Informação detalhada do combo;
- Status: Representa a disponibilidade de um combo (disponível ou esgotado). O status pode ser alterado manualmente por um usuário interno;
- Start Date: Define o momento em que o combo fica disponível no catálogo;
- End Date: Define o momento em que o combo fica indisponível.

Para um combo ser criado, editado, deletado ou ter o status alterado, o usuário interno precisa conter a permissão `manage_promotions`.

> Modelo de dados: `combo` ainda não tem `start_date`/`end_date` no diagrama (`docs/data-modeling/data_modeling.drawio`) nem no código — precisa ser adicionado para bater com o texto acima.

### Combo Product

É a relação entre um combo e os produtos que compõem o combo. Podem ser atribuídos quantos produtos o usuário interno quiser. A relação entre combo e produtos é feita durante o fluxo de criação de um combo.

Assim como em Product/Ingredient, quando um produto que compõe o combo fica esgotado, a propagação para o status do combo **não é automática** — o usuário interno decide manualmente se o combo também deve ser marcado como esgotado.

---
## Promotions

As *Promotions* sobrescrevem o custo de um produto, por um tempo limitado. Os dados necessários para criar uma promoção são:

- Title: Titulo de exibição para a promoção;
- Price: O preço da promoção. Pode ser definida manualmente, ou calculada ao selecionar um tipo de promoção (promotion type);
- Image URL: URL da imagem que ilustra a promoção;
- Description: Uma descrição que detalha a promoção;
- Status: A disponibilidade da promoção;
- Promotion Type: Define o método para o calculo ou processamento do custo da promoção;
- Product: O produto ao qual essa promoção se aplica;
- Start Date: Define o momento em que a promoção inicia;
- End Date: Define o momento em que a promoção encerra;
- Enable Promotion Points: Define se o consumidor pode utilizar dos pontos para consumir a promoção.

Cada promoção se aplica a um único produto — para promover vários produtos ao mesmo tempo, o usuário interno cria uma promoção por produto (diferente de Combo, que é uma relação N:N com produtos).

Para uma promoção ser criada, editada, deletada ou ter o status alterado, o usuário interno precisa conter a permissão `manage_promotions`.

### Promotion Type

É a configuração do tipo de promoção. Um usuário interno, que contenha a permissão `manage_promotions`, pode criar, editar e deletar um tipo de promoção. Essa configuração é global, ou seja, as *promotion types* cadastradas no sistema, ficam disponível durante a criação de uma promoção. Os dados necessários para criar um tipo de promoção são:

- Name: Nome de identificação;
- Description: Detalhes sobre o tipo de promoção;
- Type: Tipos pré-definidos de promoção;
- Value: Valor a ser considerado no cálculo do valor da promoção, caso tenha.