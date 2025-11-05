FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy pom.xml first to cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code
COPY src ./src

# Build with retry
RUN mvn clean package -DskipTests -B || \
    (sleep 5 && mvn clean package -DskipTests -B) || \
    (sleep 10 && mvn clean package -DskipTests -B)

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY --from=build /workspace/target/notification-service-0.0.1-SNAPSHOT.jar /app/app.jar
ENV JAVA_OPTS=""
EXPOSE 8085
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
