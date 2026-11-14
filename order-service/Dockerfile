# Stage 1: build the jar with Maven (no local Maven or JDK needed to build the image).
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies first, so they're cached and only re-downloaded when pom.xml changes.
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# Stage 2: a small runtime image with only the JRE and the jar.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/order-service-*.jar app.jar
# Run as a non-root user.
RUN useradd --system --uid 1001 appuser
USER appuser
EXPOSE 8081
# Let the JVM size its heap from the container's memory limit.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "app.jar"]
