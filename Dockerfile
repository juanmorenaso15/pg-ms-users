FROM maven:3.9.9-eclipse-temurin-21-alpine AS build-common

WORKDIR /build
COPY pg-lib-common/pom.xml pg-lib-common/pom.xml
COPY pg-lib-common/src pg-lib-common/src

RUN mvn -f pg-lib-common/pom.xml -q clean install -DskipTests
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build-ms

WORKDIR /build
COPY --from=build-common /root/.m2 /root/.m2

COPY pg-ms-users/pom.xml pg-ms-users/pom.xml
COPY pg-ms-users/src pg-ms-users/src

RUN mvn -f pg-ms-users/pom.xml -q clean package -DskipTests
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build-ms /build/pg-ms-users/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]