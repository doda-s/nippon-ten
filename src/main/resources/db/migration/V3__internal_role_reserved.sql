-- Marca a role super_admin, criada no startup pelo SuperAdminBootstrap. Role reservada não pode
-- ser editada, desativada, deletada nem virar default, e só um super_admin a atribui.
alter table internal_role add column reserved boolean not null default false;
