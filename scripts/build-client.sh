#!/usr/bin/env bash
# Gera o client TypeScript a partir de target/openapi/openapi.yaml (rode `./mvnw package` antes) e
# publica no npm. A versão vem de nippon.client.version em application.properties.
#
# Variáveis de ambiente:
#   CLIENT_SNAPSHOT  true (padrão) publica 0.1.0-SNAPSHOT.<timestamp> na tag npm "snapshot";
#                    false publica a versão exata na tag "latest".
#   CLIENT_PUBLISH   true (padrão) publica; false só gera e compila (útil sem token do npm).
#   NPM_DIST_TAG     sobrescreve a tag npm escolhida acima.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_DIR="$ROOT_DIR/target/openapi-clients/node-client"
PROPERTIES="$ROOT_DIR/src/main/resources/application.properties"

SNAPSHOT="${CLIENT_SNAPSHOT:-true}"
PUBLISH="${CLIENT_PUBLISH:-true}"

VERSION="$(sed -n 's/^nippon\.client\.version[[:space:]]*=[[:space:]]*//p' "$PROPERTIES" | tr -d '[:space:]')"
if [[ -z "$VERSION" ]]; then
  echo "nippon.client.version não encontrado em $PROPERTIES" >&2
  exit 1
fi

if [[ "$SNAPSHOT" == "true" ]]; then
  DIST_TAG="${NPM_DIST_TAG:-snapshot}"
else
  DIST_TAG="${NPM_DIST_TAG:-latest}"
fi

cd "$ROOT_DIR"

# Usa o openapi-generator-cli global se existir; senão o do npm, na versão de openapitools.json.
if command -v openapi-generator-cli >/dev/null 2>&1; then
  GENERATOR=(openapi-generator-cli)
else
  GENERATOR=(npx --yes @openapitools/openapi-generator-cli)
fi

rm -rf "$OUTPUT_DIR"

"${GENERATOR[@]}" generate \
  -i ./target/openapi/openapi.yaml \
  -g typescript-axios \
  -o "$OUTPUT_DIR" \
  --git-user-id doda-s \
  --git-repo-id nippon-ten \
  --additional-properties=apiPackage=nippon-ten,npmName=nippon-ten,npmVersion="$VERSION",snapshot="$SNAPSHOT",licenseName=GPL-3.0

cd "$OUTPUT_DIR"

# O npm install roda o "prepare" do pacote gerado, que compila o TypeScript em dist/.
npm install
npm pkg fix

PACKAGE_VERSION="$(node -p "require('./package.json').version")"
echo "Client nippon-ten@$PACKAGE_VERSION gerado em $OUTPUT_DIR (tag npm: $DIST_TAG)"

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  echo "version=$PACKAGE_VERSION" >>"$GITHUB_OUTPUT"
  echo "dist-tag=$DIST_TAG" >>"$GITHUB_OUTPUT"
fi

if [[ "$PUBLISH" != "true" ]]; then
  echo "CLIENT_PUBLISH=$PUBLISH: publicação pulada."
  exit 0
fi

npm publish --access public --tag "$DIST_TAG"
