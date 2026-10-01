#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_DIR="$ROOT_DIR/target/openapi-clients/node-client"

cd "$ROOT_DIR"

openapi-generator-cli generate \
  -i ./target/openapi/openapi.yaml \
  -g typescript-axios \
  -o "$OUTPUT_DIR" \
  --git-user-id doda-s \
  --git-repo-id nippon-ten \
  --additional-properties=apiPackage=nippon-ten,npmName=nippon-ten,snapshot=true,licenseName=GPL-3.0

cd "$OUTPUT_DIR"

npm install
npm pkg fix

npm publish --access public --tag snapshot
