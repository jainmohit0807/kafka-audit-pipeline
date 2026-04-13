FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src

RUN apk add --no-cache maven \
    && mvn clean package -DskipTests -q \
    && mv target/*.jar app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache wget && addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=build /app/app.jar app.jar

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
    CMD wget -qO- http://localhost:8080/api/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
