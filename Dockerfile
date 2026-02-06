FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml ./
RUN mvn -q -e -B dependency:go-offline

COPY src ./src

RUN mvn -q -e -B clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

ARG JAR_FILE=/app/target/*.jar
COPY --from=builder ${JAR_FILE} app.jar

EXPOSE 8087

ENV SPRING_PROFILES_ACTIVE=dev

ENTRYPOINT ["java", "-jar", "app.jar"]
