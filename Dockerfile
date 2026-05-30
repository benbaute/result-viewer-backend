FROM gradle:8-jdk23 AS builder
WORKDIR /app


COPY gradlew build.gradle.kts settings.gradle.kts /app/
COPY gradle /app/gradle

COPY common/build.gradle.kts /app/common/
COPY common/src /app/common/src

COPY osmPlanet/build.gradle.kts /app/osmPlanet/
COPY osmPlanet/src /app/osmPlanet/src

COPY rides/build.gradle.kts /app/rides/
COPY rides/src /app/rides/src

COPY valhalla/build.gradle.kts /app/valhalla/
COPY valhalla/src /app/valhalla/src
COPY src /app/src


RUN ./gradlew bootJar -x test --no-daemon


FROM eclipse-temurin:23-jre-alpine
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]