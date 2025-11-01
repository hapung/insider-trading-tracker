
FROM gradle:jdk17-jammy AS builder

WORKDIR /app

COPY ./backend /app/backend

WORKDIR /app/backend

RUN chmod +x ./gradlew

RUN ./gradlew build -x test

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

EXPOSE 8080

COPY --from=builder /app/backend/build/libs/backend-0.0.1-SNAPSHOT.jar ./app.jar

ENTRYPOINT ["java", "-jar", "./app.jar"]