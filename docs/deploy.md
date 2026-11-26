A API roda no Google Cloud Run. Por enquanto só existe o ambiente de **staging**; produção ainda não foi criada.

## Staging

| Recurso | Valor |
| --- | --- |
| Projeto GCP | `nippon-ten-staging` (região `southamerica-east1`, São Paulo) |
| Serviço Cloud Run | `nippon-ten-api` — <https://nippon-ten-api-637299454303.southamerica-east1.run.app> |
| Imagens | Artifact Registry `southamerica-east1-docker.pkg.dev/nippon-ten-staging/nippon-ten/api:<commit>` |
| Banco | Neon, database `nippon_staging` (endpoint direto, sem `-pooler`), separado do banco de dev |
| Front | <https://niponten-front.vercel.app> (`FRONT_ORIGINS`) |

O serviço roda com a service account `nippon-ten-api@nippon-ten-staging.iam.gserviceaccount.com`, 1 CPU, 1 GiB, de 0 a 1 instância e acesso público. Com zero instâncias, a primeira request sobe a JVM e roda o Flyway (cerca de 10 s). O limite de uma instância mantém consistentes os limitadores e o cache de sessão, que ficam em memória.

### Configuração do serviço

Variáveis e secrets ficam no próprio serviço Cloud Run. A pipe troca a imagem e aplica as linhas marcadas com *pipe*; as demais foram configuradas uma vez e o deploy as mantém.

| Variável | Origem |
| --- | --- |
| `FRONT_ORIGINS` | Valor fixo: `https://niponten-front.vercel.app` |
| `FRONT_BASE_URL` | *pipe*, valor fixo: `https://niponten-front.vercel.app` |
| `QUARKUS_SCHEDULER_ENABLED` | *pipe*, `false` (ver [Jobs agendados](#jobs-agendados)) |
| `MERCADOPAGO_ACCESS_TOKEN`, `MERCADOPAGO_WEBHOOK_SECRET` | *pipe*, secrets `mercadopago-access-token`, `mercadopago-webhook-secret`, com valor vindo do GitHub |
| `MAILER_FROM`, `MAILER_USERNAME`, `MAILER_PASSWORD` | *pipe*, secrets `mailer-from`, `mailer-username`, `mailer-password`, com valor vindo do GitHub |
| `NIPPON_JOBS_TOKEN` | *pipe*, secret `nippon-jobs-token` (gerado no GCP, não fica no GitHub) |
| `GOOGLE_MAPS_API_KEY` | *pipe*, secret `google-maps-api-key` (fica no GCP, não no GitHub; ver [`delivery.md`](delivery.md)) |
| `DB_JDBC_URL`, `DB_USERNAME`, `DB_PASSWORD` | Secrets `db-jdbc-url`, `db-username`, `db-password` |
| `NIPPON_BOOTSTRAP_ADMIN_EMAIL`, `NIPPON_BOOTSTRAP_ADMIN_PASSWORD` | *pipe*, secrets `bootstrap-admin-email`, `bootstrap-admin-password`, com valor vindo do GitHub |
| `JWT_PRIVATE_KEY_LOCATION`, `JWT_PUBLIC_KEY_LOCATION` | `/secrets/jwt-private/key.pem` e `/secrets/jwt-public/key.pem`, arquivos montados dos secrets `jwt-private-key` e `jwt-public-key` |

Os secrets ficam no Secret Manager do projeto, e só a service account do serviço tem acesso de leitura, além da `github-deployer` nos que vêm do GitHub.

Trocar um secret é criar uma versão nova (`gcloud secrets versions add <nome> --data-file=-`) e fazer um deploy, porque a revisão lê `latest` só quando sobe. Os secrets que vêm do GitHub não se trocam assim: troque no GitHub e rode a pipe, senão o próximo deploy grava o valor do GitHub por cima.

### Secrets do GitHub

O Mercado Pago, o SMTP e o admin inicial são configurados no GitHub, em Settings > Environments > `staging` > *Environment secrets*:

| Secret | Valor |
| --- | --- |
| `MERCADOPAGO_ACCESS_TOKEN` | Access token de teste do Mercado Pago (`TEST-...`) |
| `MERCADOPAGO_WEBHOOK_SECRET` | Assinatura secreta do webhook, do painel do Mercado Pago |
| `MAILER_FROM` | Remetente dos e-mails de pedido |
| `MAILER_USERNAME`, `MAILER_PASSWORD` | Login SMTP (Gmail por padrão, `smtp.gmail.com:587`; com o Gmail, a senha é uma senha de app) |
| `NIPPON_BOOTSTRAP_ADMIN_EMAIL`, `NIPPON_BOOTSTRAP_ADMIN_PASSWORD` | Email e senha do `super_admin` criado pelo bootstrap (ver *Bootstrap* em [`user-management.md`](user-management.md)) |

O admin inicial só é criado quando nenhum usuário interno tem a role `super_admin`. Depois disso, trocar esses dois secrets não muda o email nem a senha de quem já existe: a senha se troca pela API, como a de qualquer usuário. Os secrets passam a valer de novo num banco novo, ou se o `super_admin` for removido.

O deploy espera esses secrets: se faltar algum, a pipe falha no primeiro passo, antes de gerar a imagem, e diz qual falta. Com todos presentes, ela grava no Secret Manager só os que mudaram (uma versão nova por mudança) e faz o deploy apontando para eles. A service account `github-deployer` tem leitura e `secretVersionAdder` só nesses sete secrets.

No painel do Mercado Pago, a URL do webhook de staging é `https://nippon-ten-api-637299454303.southamerica-east1.run.app/payments/webhooks/mercadopago`.

## Jobs agendados

O Cloud Run só aloca CPU durante uma request, e com zero instâncias não há processo rodando. Por isso o scheduler interno do Quarkus fica desligado no serviço (`QUARKUS_SCHEDULER_ENABLED=false`), e os jobs são disparados de fora pelo **Cloud Scheduler**, por HTTP:

| Job do Cloud Scheduler | Frequência | Chama |
| --- | --- | --- |
| `payment-reconciliation` | A cada minuto (`* * * * *`, `America/Sao_Paulo`) | `POST /jobs/payment-reconciliation` → `PaymentService.reconcile()` |
| `geocode-cache-purge` | Todo dia às 4h (`0 4 * * *`, `America/Sao_Paulo`) | `POST /jobs/geocode-cache-purge` → `GeocodingService.purgeExpired()` |

Cada job do scheduler interno tem uma rota em `/jobs` e um job no Cloud Scheduler; um `@Scheduled` novo sem esse par não roda no Cloud Run. Um job novo no Cloud Scheduler é criado pausado (`gcloud scheduler jobs create http ...` seguido de `gcloud scheduler jobs pause ...`) e entra na lista do passo *Ativa os jobs do Cloud Scheduler* da pipe, que o ativa depois do deploy, quando a rota já está no ar. As rotas são públicas e exigem o header `X-Job-Token` igual a `nippon.jobs.token` (`NIPPON_JOBS_TOKEN`); qualquer outro valor responde `401`, e sem token configurado nenhuma chamada é aceita. O token fica no secret `nippon-jobs-token` e no header dos jobs do Cloud Scheduler. Uma chamada que chega com outra rodada do mesmo job ainda em andamento responde `204` sem fazer nada. Em dev e fora do Cloud Run, o `PaymentReconciliationJob` e o `GeocodeCachePurgeJob` continuam rodando pelo scheduler interno.

Trocar o token é criar uma versão nova do secret, fazer um deploy e atualizar cada job (`gcloud scheduler jobs update http <job> --location southamerica-east1 --update-headers X-Job-Token=<token>`).

A chamada a cada minuto mantém o banco acordado: o Neon não suspende a computação por inatividade enquanto o job roda, o que conta nas horas de computação do plano.

## Deploy automático

A pipe `.github/workflows/deploy-staging.yml` roda depois que o **CI** termina com sucesso num push na `main`, e também manualmente (Actions > *Deploy staging* > *Run workflow*, só a partir da `main`). Ela confere os [secrets do GitHub](#secrets-do-github), empacota a aplicação, publica a imagem com a tag do commit, sincroniza os secrets com o Secret Manager, faz o deploy de uma revisão nova no Cloud Run e ativa os [jobs agendados](#jobs-agendados) que estiverem pausados.

A autenticação no GCP usa Workload Identity Federation, sem chave JSON no repositório:

- Pool `github`, provider `github-oidc`, que só aceita tokens do repositório `doda-s/nippon-ten` na ref `refs/heads/main`.
- Service account `github-deployer@nippon-ten-staging.iam.gserviceaccount.com`, com `roles/run.developer` no projeto, `roles/artifactregistry.writer` no repositório `nippon-ten`, `roles/iam.serviceAccountUser` na service account do serviço e `roles/secretmanager.secretAccessor` mais `roles/secretmanager.secretVersionAdder` nos secrets que vêm do GitHub, e o papel customizado `schedulerJobEnabler` (só `cloudscheduler.jobs.get` e `cloudscheduler.jobs.enable`), que deixa a pipe ativar um job, mas não criar, alterar ou apagar.

## Deploy manual

Com o gcloud autenticado e o Docker rodando:

```shell
./mvnw package -DskipTests
IMG=southamerica-east1-docker.pkg.dev/nippon-ten-staging/nippon-ten/api:$(git rev-parse HEAD)
gcloud auth configure-docker southamerica-east1-docker.pkg.dev
docker build -f src/main/docker/Dockerfile.jvm -t $IMG .
docker push $IMG
gcloud run deploy nippon-ten-api --project nippon-ten-staging --region southamerica-east1 --image $IMG
```

## Rollback e observação

- Cada deploy é uma revisão. Para voltar, mande o tráfego para uma revisão anterior: `gcloud run services update-traffic nippon-ten-api --region southamerica-east1 --to-revisions <revisão>=100`.
- Métricas, logs e revisões ficam no console, em Cloud Run > `nippon-ten-api`.

## Limitações conhecidas

- **Cookie de refresh entre sites.** Front (`vercel.app`) e API (`run.app`) estão em sites diferentes, então o `nt_refresh` é um cookie de terceiro. O Safari (e o iOS) bloqueia esse cookie por padrão, e o refresh falha nesses navegadores. A saída é colocar front e API sob o mesmo domínio próprio.
- **Sem health check.** O Cloud Run usa o probe TCP padrão; a aplicação não tem `quarkus-smallrye-health`.
- **Swagger UI.** Em produção o Quarkus não inclui o Swagger UI, então `/api` responde 404. O schema continua em `/q/openapi`.
- **E-mail depois da resposta.** O e-mail de confirmação do pedido é enviado depois que a resposta sai (`AFTER_SUCCESS` com o `ReactiveMailer`). Com a CPU alocada só durante requests, o envio pode atrasar até a próxima request ou falhar. Se isso aparecer no staging, a saída é CPU sempre alocada (`--no-cpu-throttling`) ou esperar o envio dentro da request.
