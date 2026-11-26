-- Telefone do cliente: o contato da equipe na retirada e na entrega. Obrigatório no cadastro e
-- na edição; as contas de antes ficam sem ele até o cliente atualizar.
alter table client add column phone varchar(30);
