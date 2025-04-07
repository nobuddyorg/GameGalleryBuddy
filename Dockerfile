FROM gradle:8.13.0-jdk21 AS build
WORKDIR '/app'
COPY src src
COPY build.gradle build.gradle
COPY settings.gradle settings.gradle
COPY gradle gradle
COPY gradlew gradlew
RUN ./gradlew build --no-daemon --stacktrace --info

FROM openjdk:21-jdk-slim
WORKDIR '/app'
COPY --from=build /app/build/libs/boardgames-0.0.1.jar .
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/boardgames-0.0.1.jar"]
