-- Instantes técnicos passam a ter fuso (timestamptz) e a ser lidos como Instant em UTC. Os valores
-- existentes são interpretados como UTC; não existe banco de produção, e o de dev é recriado a cada
-- start.
-- Início e fim de combos e promoções continuam timestamp sem fuso: são horários locais do
-- restaurante (nippon.business-zone).
alter table "user"
    alter column created_at type timestamptz(6) using created_at at time zone 'UTC';

alter table refresh_token
    alter column expires_at type timestamptz(6) using expires_at at time zone 'UTC',
    alter column created_at type timestamptz(6) using created_at at time zone 'UTC',
    alter column revoked_at type timestamptz(6) using revoked_at at time zone 'UTC';
