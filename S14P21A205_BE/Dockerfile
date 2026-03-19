FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

ENV TZ=Asia/Seoul

COPY build/libs/app.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "/app/app.jar"]
