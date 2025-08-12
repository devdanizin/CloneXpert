# Usar imagem Maven para buildar
FROM maven:3.9.3-eclipse-temurin-17 AS build

WORKDIR /app

# Copiar os arquivos do projeto
COPY pom.xml .
COPY src ./src

# Buildar o jar
RUN mvn clean package -DskipTests

# Rodar o app com JRE
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copiar o jar do estágio de build
COPY --from=build /app/target/*.jar app.jar

# Expor porta
EXPOSE 8080

# Rodar o jar
ENTRYPOINT ["java","-jar","app.jar"]