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
ENV SPRING_PROFILES_ACTIVE=prd
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
