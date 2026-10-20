# 1단계: JDK로 실행용 jar를 만든다
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null   # 의존성만 먼저 받아 레이어 캐시를 살린다
COPY src src
RUN ./gradlew --no-daemon bootJar

# 2단계: 실행에는 JRE와 jar만 남긴다
FROM eclipse-temurin:17-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*   # compose healthcheck용
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
