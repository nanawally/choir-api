# Stage 1: Build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew buildFatJar --no-daemon

# Stage 2: Run
FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y ghostscript && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/build/libs/choir-api-all.jar app.jar
EXPOSE 8081
CMD ["java", "-jar", "app.jar"]
