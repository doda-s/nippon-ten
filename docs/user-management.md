O *User* se trata de uma entidade comum que pode ser relacionado com uma entidade *Client* ou *Internal*. Essas relações definem usuários internos e usuários clientes. Cada um tem regras de negócios diferentes para *CRUD* e operações.

## Client User

O *Client User* representa um cliente que utiliza e consome do serviço fornecido pelo sistema. 

Ao criar uma conta o usuário precisa fornecer os seguintes dados:

- Email
- Password
- Name
- Last Name
- CPF (opcional)
- Endereço (opcional)

O email é único no sistema (sem diferenciar maiúsculas de minúsculas), tanto para *Client User* quanto para *Internal User*. Um *User* é sempre **ou** cliente **ou** interno, nunca os dois.

Fluxo para criar um *Client User*:

```mermaid
flowchart TD

A[Endpoint Create User]

B[Create User]

C[Create Client]

D[Client and user relationship]

A --> B

B --> C

C --> D
```

### User Address

Um usuário opcionalmente pode adicionar um endereço ao criar uma conta. Mas ele se torna obrigatório ao realizar um pedido. As informações necessárias para o *user address* são:

- Street Address
- Number
- CEP
- Complement

---
## Internal User

O *Internal User* representa um usuário com permissões especiais para gerenciamento do sistema e procedimentos. Apenas usuários internos com permissão de `manage_users` podem criar, editar e desativar usuários. Deletar um *Internal User* só pode ser feito por um usuário com a role reservada `super_admin` (ver [[#Bootstrap|Bootstrap]]), após o *Internal User* ser desativado.

Desativar é uma flag `active` no *User* (vale para cliente e interno; todo usuário nasce ativo e pode ser reativado). Deletar um *Internal User* ainda ativo é recusado. Deletar um *Internal User* remove também o *User* associado.

Para criar um *Internal User* é necessário os seguintes dados:

- Email
- Password
- Name
- Last name
- CPF
- Role (opcional — se não for informada, o *Internal* recebe a role padrão; ver [[#Roles|Roles]])
- User permissions (opcional — permissões individuais, somadas às da role; mesmo catálogo de [[#Permissions|Permissions]])

Fluxo para criar um *Internal User*:

```mermaid
flowchart TD

A[Endpoint Create User] --> B[Check User Permission]

B --> C[Create User]

C --> D[Create Internal]

D --> E[Internal and user relationship]
```

### Roles

As *roles* se tratam de um conjunto de permissões que um usuário interno pode ter. Uma role pode ser criada por um usuário que tenha a permissão `manage_roles`. Para criar uma role é necessário as seguintes informações:

- Name (único entre as roles)
- Permissions (lista obrigatória, mas pode ser vazia)
- Default role (opcional, `false` por padrão)

As *permissions* são pré-definidas no sistema (catálogo fixo, ver [[#Permissions|Permissions]]) e cada role guarda a sua lista de permissões — uma role pode ter várias permissões, e uma permissão pode estar em várias roles. Uma role só aceita *permissions* válidas (existentes no catálogo); um valor fora do catálogo é recusado com `400`.

Assim como o *User*, a role tem uma flag `active`: toda role nasce ativa, a edição não altera o valor e ela pode ser desativada/reativada por `POST /internal-roles/{id}/deactivate` e `POST /internal-roles/{id}/activate`.

Existe no máximo uma **role padrão**. Ao criar ou editar uma role com `defaultRole = true`, a role padrão anterior deixa de ser padrão. A role padrão é atribuída ao *Internal* criado sem role; se nenhuma role padrão estiver configurada, essa criação é recusada (`400`).

Uma role atribuída a pelo menos um *Internal User* não pode ser deletada (`400`).

> Modelo de dados: o catálogo de *permissions* é o enum `InternalPermission` (valores em maiúsculas na API, ex.: `MANAGE_USERS`), sem tabela própria. As permissões de uma role ficam numa coluna array na própria tabela:
>
> - `internal_role(id, name UNIQUE, permissions[], default_role, active)`
> - `internal(id, user_id, internal_role_id, user_permissions[], name, last_name, cpf)` — `user_permissions` são permissões individuais do usuário interno, além das da role.
>
> O diagrama (`docs/data-modeling/data_modeling.drawio`) ainda mostra `internal_role` como `(id, name)` e `internal` sem `user_permissions` — precisa ser atualizado manualmente.

## Permissions

| Permission         | Description                                                                                                                                                                |
| ------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| manage_users       | Permissão para gerenciar usuários. Possibilita criar, editar e desativar um usuário                                                                                        |
| manage_roles       | Permissão para gerenciar roles. Possibilita criar, editar e deletar roles                                                                                                  |
| manage_products    | Permissão para gerenciar produtos. Possibilita criar, editar, deletar e alterar o status de produtos. Também permite modificar os ingredientes ao qual um produto depende. |
| manage_ingredients | Permissão para gerenciar os ingredientes cadastrados no sistema. Possibilita cadastrar, editar, deletar e alterar o status de um ingrediente.                              |
| manage_promotions  | Permissão para gerenciar os combos e promotions existentes. Possibilita criar, editar, deletar e alterar o status de um combo/promotion.                                   |

## Bootstrap

O primeiro *Internal User* do sistema não pode ser criado pelo fluxo normal, já que esse fluxo exige a permissão `manage_users` — que ninguém ainda possui. Por isso, na primeira subida do sistema, um seed/migration cria:

1. Uma role reservada `super_admin`, com todas as permissões do catálogo;
2. Um *Internal User* inicial associado a essa role.

A role `super_admin` é a única com poder de deletar um *Internal User* (ver [[#Internal User|Internal User]]). Ela não pode ser deletada nem ter suas permissões removidas por outra role.

---
## Guest User

Representa um usuário que não possui um cadastro, ou seja, não é um *Client User*. Esse usuário permite que um consumidor possa utilizar dos serviços, sem a necessidade de passar pelo fluxo de cadastro/login. Utilizar o sistema desta forma limita a experiência do usuário, pois inúmeras funcionalidades ficam indisponíveis. Funcionalidades como:

- Acúmulo de pontos;
- Utilização de pontos;
- Preenchimento automático de dados necessário ao fazer um pedido;
- Salvar endereços;
- Salvar formas de pagamento.
- Histórico de pedidos

Nenhum dado desse tipo de usuário fica persistido. Apenas o necessário para a realização de análises de negócio. Exemplos de casos onde dados de Guest User podem ser salvos:

- Histórico de pedidos geral;
- Analytics (mediante aceite do usuário).