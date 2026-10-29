@echo off
setlocal

set "ROOT_DIR=%~dp0.."
set "OUTPUT_DIR=%ROOT_DIR%\target\openapi-clients\node-client"

cd /d "%ROOT_DIR%" || exit /b 1

call openapi-generator-cli generate ^
  -i ./target/openapi/openapi.yaml ^
  -g typescript-axios ^
  -o "%OUTPUT_DIR%" ^
  --git-user-id doda-s ^
  --git-repo-id nippon-ten ^
  --additional-properties=apiPackage=nippon-ten,npmName=nippon-ten,snapshot=true,licenseName=GPL-3.0 || exit /b 1

cd /d "%OUTPUT_DIR%" || exit /b 1

call npm install || exit /b 1
call npm pkg fix || exit /b 1

call npm publish --access public --tag snapshot || exit /b 1

endlocal
