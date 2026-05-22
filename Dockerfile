# =====================================================================
# Dockerfile multi-stage para admin-tools-api
#
# Stage 1: build con Gradle 8.5 + JDK 21
# Stage 2: runtime con JRE 21 jammy
#
# La dependencia 'net.datatecsolution:admintools-core:0.1.0-SNAPSHOT'
# vive en libs/ del repo (ver libs/README.md). Para que Gradle la
# resuelva en el container, la pre-publicamos a mavenLocal de root
# (/root/.m2/repository/...) ANTES de invocar 'gradle build'.
#
# El build.gradle del repo tiene 'mavenLocal()' en repositories, asi
# que encuentra el artifact ahi.
# =====================================================================

# Stage 1: Build the application
FROM gradle:8.5-jdk21 AS build
WORKDIR /home/gradle/project
COPY . .

# Pre-publicar admintools-core a mavenLocal del container.
# El path es el esperado por Maven/Gradle:
#   ~/.m2/repository/<groupId-con-slashes>/<artifactId>/<version>/
RUN mkdir -p /root/.m2/repository/net/datatecsolution/admintools-core/0.1.0-SNAPSHOT && \
    cp libs/admintools-core-0.1.0-SNAPSHOT.jar /root/.m2/repository/net/datatecsolution/admintools-core/0.1.0-SNAPSHOT/ && \
    cp libs/admintools-core-0.1.0-SNAPSHOT.pom /root/.m2/repository/net/datatecsolution/admintools-core/0.1.0-SNAPSHOT/

RUN gradle clean build -x test --no-daemon

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /home/gradle/project/build/libs/admintools-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
