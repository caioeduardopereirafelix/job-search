# Etapa 1: compila o front (TypeScript -> JavaScript)
FROM node:20-alpine AS frontend
WORKDIR /app
COPY package.json package-lock.json tsconfig.json ./
RUN npm ci
COPY src/main/resources/static/ts ./src/main/resources/static/ts
RUN npm run build

# Etapa 2: compila o backend e empacota o jar
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
# sobrescreve o js commitado pelo que acabou de ser gerado a partir do .ts,
# assim a imagem nunca depende do js commitado estar em dia
COPY --from=frontend /app/src/main/resources/static/js ./src/main/resources/static/js
RUN mvn -q -B -DskipTests package

# Etapa 3: imagem final, so com o JRE e o jar (sem Maven/Node/codigo fonte)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app \
    && mkdir -p /data/resumes && chown -R app:app /data/resumes
COPY --from=backend /app/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
