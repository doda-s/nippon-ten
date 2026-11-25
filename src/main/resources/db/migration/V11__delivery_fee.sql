-- Taxa de entrega (docs/delivery.md). A taxa sai da distância em linha reta entre a loja e o
-- endereço, em faixas configuráveis. O CEP só dá rua, bairro, cidade e UF; a coordenada vem do
-- Google Geocoding a partir do endereço completo.

-- Configuração única (id = 1). Sem as coordenadas da loja o cálculo fica desligado e a taxa continua
-- 0, como na v1, até um usuário com manage_delivery configurar a entrega.
create table delivery_settings (
    id                 bigint           not null primary key check (id = 1),
    store_latitude     double precision check (store_latitude between -90 and 90),
    store_longitude    double precision check (store_longitude between -180 and 180),
    updated_at         timestamptz      not null,
    updated_by_user_id bigint           constraint fk_delivery_settings_user references "user" on delete set null
);

insert into delivery_settings (id, updated_at) values (1, now());

-- Faixas "até max_distance_km, cobra fee". A maior faixa é o raio de entrega.
create table delivery_fee_tier (
    delivery_settings_id bigint        not null constraint fk_delivery_fee_tier_settings references delivery_settings on delete cascade,
    max_distance_km      numeric(5, 2)  not null check (max_distance_km > 0),
    fee                  numeric(38, 2) not null check (fee >= 0),
    primary key (delivery_settings_id, max_distance_km)
);

-- Cache das consultas de CEP (ViaCEP, BrasilAPI). Evita chamar o provedor a cada cotação e segura
-- a consulta quando ele cai. street e neighborhood são nulos no CEP único de cidade pequena.
create table cep_address (
    cep          varchar(8)   not null primary key,
    street       varchar(255),
    neighborhood varchar(255),
    city         varchar(255) not null,
    state        varchar(2)   not null,
    fetched_at   timestamptz  not null
);

-- Cache das coordenadas do Google. Os termos do Google só permitem guardar lat/lng por 30 dias, por
-- isso a linha é apagada depois disso. A chave é o SHA-256 do endereço normalizado, sem o endereço
-- em claro. latitude e longitude nulas = o Google não achou o endereço.
create table geocoded_address (
    address_hash varchar(64)      not null primary key,
    latitude     double precision,
    longitude    double precision,
    geocoded_at  timestamptz      not null
);

create index ix_geocoded_address_geocoded_at on geocoded_address (geocoded_at);

-- Bairro, cidade e UF vêm da consulta do CEP ao salvar o endereço. Nos endereços antigos ficam
-- nulos, e o pedido de entrega consulta o CEP na hora.
alter table user_address
    add column neighborhood varchar(255),
    add column city         varchar(255),
    add column state        varchar(2);

-- Cópia do endereço completo e a distância usada no cálculo da taxa, para o histórico não mudar
-- quando a configuração mudar.
alter table orders
    add column neighborhood         varchar(255),
    add column city                 varchar(255),
    add column state                varchar(2),
    add column delivery_distance_km numeric(6, 2);
