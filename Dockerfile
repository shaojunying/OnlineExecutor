# syntax=docker/dockerfile:1

# ---- Build ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline
COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests \
    && cp target/online-executor-*.jar /app.jar

# ---- Run ----
# The server compiles user code with javac, so the runtime image must be a JDK, not a JRE.
FROM eclipse-temurin:21-jdk
RUN useradd --system --uid 10001 --home-dir /app executor
WORKDIR /app
COPY --from=build /app.jar app.jar
USER executor
EXPOSE 8080
# process mode: each run starts a child JVM inside this container.
# For docker mode, see "部署" in README.md.
ENV EXECUTOR_MODE=process
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=50", "-jar", "app.jar"]
