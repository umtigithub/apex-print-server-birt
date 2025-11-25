# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copiar arquivos de configuração do Maven
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Copiar código fonte
COPY src ./src

# Copiar BIRT Runtime (necessário para o build)
COPY repo ./repo

# Build da aplicação
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

# Instalar bash (necessário para genReport.sh)
RUN apk add --no-cache bash

WORKDIR /app

# Criar usuário não-root para segurança
RUN addgroup -S spring && adduser -S spring -G spring

# Copiar BIRT Runtime
COPY --from=build /app/repo ./repo

# Copiar JAR da aplicação
COPY --from=build /app/target/birtprintserver-0.0.1-SNAPSHOT.jar app.jar

# Garantir que o script genReport.sh seja executável
RUN chmod +x /app/repo/birt-runtime-4.21.0/ReportEngine/genReport.sh

# Criar diretório para logs e temporários
RUN mkdir -p /app/repo/birt-runtime-4.21.0/ReportEngine/tmpdir && \
    chown -R spring:spring /app

# Mudar para usuário não-root
USER spring:spring

# Expor porta
EXPOSE 8080

# Executar aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]



