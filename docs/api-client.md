O client Node da API é gerado a partir do schema OpenAPI (`target/openapi/openapi.yaml`, gravado pelo Quarkus no build) e publicado no npm como o pacote `nippon-ten`.

## Instalação

O client é publicado como versão *snapshot* (ex.: `0.1.0-SNAPSHOT.202610011825`), na tag `snapshot` do npm. Para instalar:

```shell
npm install nippon-ten@snapshot
```

## Publicação

Os scripts `scripts/build-client.sh` (Linux/macOS) e `scripts/build-client.bat` (Windows) geram o client com o OpenAPI Generator (`typescript-axios`) em `target/openapi-clients/node-client`, instalam as dependências e publicam o pacote com `npm publish --access public --tag snapshot`.
