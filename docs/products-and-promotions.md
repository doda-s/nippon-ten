## Product

Um *Product* representa um item/serviço oferecido pelo cliente do sistema. O *Product* é composto pelas seguintes informações:

- Name: É o nome que será exibido para os usuários que consomem os produtos/serviços do cliente do sistema.
- Product Size: É a configuração que define qual o tamanho do produto.
- Image URL: URL da imagem ilustrativa do produto.
- Description: Descrição com os detalhes do produto.
- Status: Status representa a disponibilidade do produto (disponível ou esgotado). O status pode ser alterado manualmente por um usuário interno.

O produto pode ter seu status alterado caso um ingrediente se esgote. Ao alterar o status de um produto para esgotado, opcionalmente o usuário interno pode alterar os staus dos combos e promoções que dependem do produto em específico.

Para a criação de um produto, o usuário interno precisa conter a [[user-management#Permissions|permissão]] `product_management` para poder criar, editar, deletar e definir o status um produto.

### Product Ingredients

*Product Ingredients* representa a relação entre os ingredientes cadastrados, e os ingredientes necessários para o produto. Essa relação é feita durante a criação de um produto. Nesse fluxo o usuário interno define quais são os ingredientes que o produto depende.

### Product Size

O *Product Size* representa o tamanho do produto. Um usuário interno pode criar e definir os tamanhos existentes para os produtos. Essa configuração é global, ou seja, durante a criação de um produto, a definição dos tamanhos é compartilhada. Para cada produto podem ser selecionados tamanhos específicos, mas esses tamanhos são cadastrados globalmente. Para poder configurar um *Product Size* o usuário interno precisa conter a [[user-management#Permissions|permissão]] `product_management`.

---
## Ingredient

*Ingredient* representa a matéria prima para os produtos serem confeccionados. Os dados que compõem um *ingredient* são:

- Name: É o nome para identificação do ingrediente.
- Description (opcional): Uma descrição detalhada do ingrediente.
- Image URL: URL da imagem de ilustração do ingrediente.
- Price: É o preço usado para calcular o acréscimo ao adicionar um ingrediente como adicional em um pedido
- Status: Status representa a disponibilidade do produto (disponível ou esgotado). O status pode ser alterado manualmente por um usuário interno.

Quando um ingrediente é marcado como esgotado, opcionalmente o usuário interno pode marcar todos os produtos que dependem desse ingrediente como esgotado. Essa funcionalidade ajuda no gerenciamento de disponibilidade dos produtos.

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

### Combo Product

É a relação entre um combo e os produtos que compõem o combo. Podem ser atribuídos quantos produtos o usuário interno quiser.  Os produtos relacionados com o combo podem alterar o status de disponibilidade do combo. A relação entre combo e produtos é feita durante o fluxo de criação de um combo.

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

Para um combo ser criado, editado, deletado ou ter o status alterado, o usuário interno precisa conter a permissão `manage_promotions`.

### Promotion Type

É a configuração do tipo de promoção. Um usuário interno, que contenha a permissão `manage_promotions`, pode criar, editar e deletar um tipo de promoção. Essa configuração é global, ou seja, as *promotion types* cadastradas no sistema, ficam disponível durante a criação de uma promoção. Os dados necessários para criar um tipo de promoção são:

- Name: Nome de identificação;
- Description: Detalhes sobre o tipo de promoção;
- Type: Tipos pré-definidos de promoção;
- Value: Valor a ser considerado no cálculo do valor da promoção, caso tenha.