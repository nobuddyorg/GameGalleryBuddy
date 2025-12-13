FROM gradle:9.1.0-jdk21 AS build
WORKDIR /app
COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle settings.gradle
COPY build.gradle build.gradle
COPY src src
RUN ./gradlew build -x test --no-daemon --stacktrace --info

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/build/libs/boardgames-0.0.1.jar .
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/boardgames-0.0.1.jar"]
