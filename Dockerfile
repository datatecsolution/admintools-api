# Stage 1: Build the application
FROM gradle:8.5-jdk21 AS build
WORKDIR /home/gradle/project
COPY . .
RUN gradle clean build -x test --no-daemon

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /home/gradle/project/build/libs/admintools-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
