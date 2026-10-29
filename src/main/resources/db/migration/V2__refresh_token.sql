-- Refresh tokens opacos. Só o SHA-256 do token é gravado; o valor em claro vive apenas no cookie.
-- Cada refresh revoga o token usado e aponta para o que o substituiu (replaced_by_id).
-- session_id é o mesmo para toda a cadeia de rotação de um login; o access token carrega esse id
-- (claim sid) e só vale enquanto a sessão tiver um refresh token ativo.

create sequence refresh_token_seq start with 1 increment by 50;

create table refresh_token (
    id             bigint      not null primary key,
    user_id        bigint      not null constraint fk_refresh_token_user references "user",
    session_id     uuid        not null,
    token_hash     varchar(64) not null unique,
    expires_at     timestamp(6) not null,
    created_at     timestamp(6) not null,
    revoked_at     timestamp(6),
    replaced_by_id bigint constraint fk_refresh_token_replaced_by references refresh_token
);

create index idx_refresh_token_user on refresh_token (user_id);
create index idx_refresh_token_session on refresh_token (session_id);
