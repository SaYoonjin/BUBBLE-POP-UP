FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY build/libs/app.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
