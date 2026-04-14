# === Build Stage ===
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# === Run Stage ===
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
RUN mkdir -p /app/uploads
# SPRING_PROFILES_ACTIVE is set by docker-compose (local or prod)
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
