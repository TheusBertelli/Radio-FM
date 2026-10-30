#!/usr/bin/env sh
# Inicialização Gradle: baixa o wrapper oficial (com SHA-256) na primeira execução.
set -eu
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
BASE='https://services.gradle.org/distributions/gradle-8.11.1-wrapper.jar'
if [ ! -f "$JAR" ]; then
  echo 'Obtendo Gradle Wrapper oficial (primeira execução requer internet)...'
  curl --fail --location --silent --show-error "$BASE" -o "$JAR"
fi
CHECKSUM=$(curl --fail --location --silent --show-error "$BASE.sha256" | tr -d '\r\n ') || exit 1
if command -v sha256sum >/dev/null 2>&1; then
  ACTUAL=$(sha256sum "$JAR" | cut -d ' ' -f 1)
elif command -v shasum >/dev/null 2>&1; then
  ACTUAL=$(shasum -a 256 "$JAR" | cut -d ' ' -f 1)
else
  echo 'sha256sum ou shasum é necessário para verificar o wrapper.' >&2; exit 1
fi
if [ "$ACTUAL" != "$CHECKSUM" ]; then
  echo 'Falha na verificação SHA-256 do Gradle Wrapper!' >&2; rm -f "$JAR"; exit 1
fi
exec java -classpath "$JAR" org.gradle.wrapper.GradleWrapperMain "$@"
