FROM eclipse-temurin:17-jdk-jammy

ENV SPRING_APPLICATION_NAME=Clone

WORKDIR /app

COPY target/Clone-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]