FROM eclipse-temurin:17-jdk-jammy

# Instala dependências necessárias para rodar Playwright (navegadores no Linux)
RUN apt-get update && apt-get install -y \
  libglib2.0-0 \
  libnss3 \
  libnspr4 \
  libatk1.0-0 \
  libatk-bridge2.0-0 \
  libcups2 \
  libdrm2 \
  libdbus-1-3 \
  libxcb1 \
  libxkbcommon0 \
  libatspi2.0-0 \
  libx11-6 \
  libxcomposite1 \
  libxdamage1 \
  libxext6 \
  libxfixes3 \
  libxrandr2 \
  libgbm1 \
  libpango-1.0-0 \
  libcairo2 \
  libasound2 \
  && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline

COPY src ./src

RUN ./mvnw clean package -DskipTests
RUN cp target/*.jar app.jar

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]