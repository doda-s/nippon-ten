-- Endereço da loja na configuração da entrega (docs/delivery.md). Digitar latitude e longitude era
-- difícil para quem configura a loja: agora ela é cadastrada pelo endereço, e a coordenada sai do
-- Google Geocoding, pelo mesmo cache de 29 dias dos endereços dos clientes. Os termos do Google não
-- deixam guardar essa coordenada por mais de 30 dias, então ela não fica aqui.
--
-- store_latitude e store_longitude passam a ser o pino posto à mão no mapa, que tem prioridade sobre
-- o endereço. A coordenada já configurada antes desta mudança fica como pino, e o cálculo continua
-- ligado sem endereço até alguém salvar a configuração de novo.
alter table delivery_settings
    add column store_street_address varchar(255),
    add column store_number         varchar(255),
    add column store_complement     varchar(255),
    add column store_cep            varchar(255),
    add column store_neighborhood   varchar(255),
    add column store_city           varchar(255),
    add column store_state          varchar(2),
    -- O endereço vem inteiro ou não vem: rua, número, CEP, cidade e UF.
    add constraint ck_delivery_settings_store_address check (
        (store_street_address is null and store_number is null and store_cep is null
            and store_city is null and store_state is null)
        or (store_street_address is not null and store_number is not null and store_cep is not null
            and store_city is not null and store_state is not null)),
    add constraint ck_delivery_settings_store_pin check (
        (store_latitude is null) = (store_longitude is null));
