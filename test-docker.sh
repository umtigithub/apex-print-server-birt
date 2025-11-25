#!/bin/bash

# Cores para output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}Construindo imagem Docker...${NC}"
docker build -t birtprintserver:latest .

if [ $? -ne 0 ]; then
    echo -e "${RED}Erro ao construir imagem!${NC}"
    exit 1
fi

echo -e "${GREEN}Imagem construída com sucesso!${NC}"

echo -e "${YELLOW}Parando container existente (se houver)...${NC}"
docker stop birtprintserver 2>/dev/null || true
docker rm birtprintserver 2>/dev/null || true

echo -e "${YELLOW}Iniciando container...${NC}"
docker run -d \
  --name birtprintserver \
  -p 8080:8080 \
  birtprintserver:latest

echo -e "${YELLOW}Aguardando aplicação iniciar...${NC}"
sleep 15

echo -e "${YELLOW}Testando endpoint /report/generate...${NC}"
curl -v -o /tmp/report-docker.pdf \
  -F "templateFile=@src/main/resources/templates/new_report_1.rptdesign" \
  -F "xmlData=@src/main/resources/templates/query_agenda_participante (3).xml" \
  http://localhost:8080/report/generate

HTTP_CODE=$?

if [ $HTTP_CODE -eq 0 ]; then
    if [ -f /tmp/report-docker.pdf ]; then
        PDF_SIZE=$(stat -c%s /tmp/report-docker.pdf 2>/dev/null || stat -f%z /tmp/report-docker.pdf 2>/dev/null)
        if [ $PDF_SIZE -gt 0 ]; then
            echo -e "${GREEN}✓ PDF gerado com sucesso! Tamanho: ${PDF_SIZE} bytes${NC}"
            echo -e "${GREEN}Arquivo salvo em: /tmp/report-docker.pdf${NC}"
            
            # Tentar extrair texto do PDF se pdftotext estiver disponível
            if command -v pdftotext &> /dev/null; then
                echo -e "${YELLOW}Conteúdo do PDF:${NC}"
                pdftotext /tmp/report-docker.pdf - | head -20
            fi
        else
            echo -e "${RED}✗ PDF gerado está vazio!${NC}"
        fi
    else
        echo -e "${RED}✗ Arquivo PDF não foi criado!${NC}"
    fi
else
    echo -e "${RED}✗ Erro ao fazer requisição!${NC}"
fi

echo -e "${YELLOW}Logs do container:${NC}"
docker logs --tail 50 birtprintserver



