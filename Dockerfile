FROM eclipse-temurin:23-jre-alpine
WORKDIR /app
# Only copy the resulting JAR build with gradle, nothing else
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]