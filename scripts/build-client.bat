@echo off
rem Gera o client TypeScript a partir de target\openapi\openapi.yaml (rode "mvnw package" antes) e
rem publica no npm como snapshot. A versao vem de nippon.client.version em application.properties.
setlocal

set "ROOT_DIR=%~dp0.."
set "OUTPUT_DIR=%ROOT_DIR%\target\openapi-clients\node-client"
set "PROPERTIES=%ROOT_DIR%\src\main\resources\application.properties"

set "VERSION="
for /f "tokens=1,* delims==" %%a in ('findstr /b /c:"nippon.client.version=" "%PROPERTIES%"') do set "VERSION=%%b"
if "%VERSION%"=="" (
  echo nippon.client.version nao encontrado em %PROPERTIES% 1>&2
  exit /b 1
)

cd /d "%ROOT_DIR%" || exit /b 1

call openapi-generator-cli generate ^
  -i ./target/openapi/openapi.yaml ^
  -g typescript-axios ^
  -o "%OUTPUT_DIR%" ^
  --git-user-id doda-s ^
  --git-repo-id nippon-ten ^
  --additional-properties=apiPackage=nippon-ten,npmName=nippon-ten,npmVersion=%VERSION%,snapshot=true,licenseName=GPL-3.0 || exit /b 1

cd /d "%OUTPUT_DIR%" || exit /b 1

call npm install || exit /b 1
call npm pkg fix || exit /b 1

call npm publish --access public --tag snapshot || exit /b 1

endlocal
