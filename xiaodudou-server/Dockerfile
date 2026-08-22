FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:22-jre-jammy
RUN groupadd --system --gid 10001 xdd \
    && useradd --system --uid 10001 --gid xdd --home-dir /app --shell /usr/sbin/nologin xdd
WORKDIR /app
RUN mkdir -p /app/logs && chown -R xdd:xdd /app
COPY --from=build --chown=xdd:xdd /workspace/target/xiaodudou-server.jar /app/app.jar

USER 10001:10001
EXPOSE 8080 8081
ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
