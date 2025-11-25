#!/bin/bash

echo "============================================"
echo " Instalação das dependências do BIRT"
echo "============================================"

BIRT_DIR="./repo/birt-runtime-4.21.0/ReportEngine/lib"

if [ ! -d "$BIRT_DIR" ]; then
  echo "ERRO: Pasta $BIRT_DIR não encontrada!"
  echo "Coloque os JARs do BIRT em: $BIRT_DIR"
  exit 1
fi

for jar in $BIRT_DIR/*.jar; do
  NAME=$(basename "$jar" .jar)
  echo "Instalando $NAME ..."
  mvn install:install-file \
    -Dfile="$jar" \
    -DgroupId=org.eclipse.birt.runtime \
    -DartifactId="$NAME" \
    -Dversion=4.21.0 \
    -Dpackaging=jar \
    -DgeneratePom=true \
    >/dev/null
done

echo ""
echo "============================================"
echo " Instalando dependências do Maven Central"
echo "============================================"

mvn dependency:resolve
mvn dependency:resolve-plugins

mvn install

