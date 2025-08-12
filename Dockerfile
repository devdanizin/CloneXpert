FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline

COPY src ./src

RUN ./mvnw clean package -DskipTests
RUN cp target/*.jar app.jar

EXPOSE 8080
<<<<<<< HEAD
CMD ["java", "-jar", "app.jar"]
=======
CMD ["java", "-jar", "app.jar"]
>>>>>>> 18650ccfc5544f59dc3538f79e2b2e55106f255a
