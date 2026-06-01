# 轻语 QingYu — 后端 Dockerfile（多阶段构建）
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/xueren-backend-1.0.0.jar app.jar
COPY src/main/resources/application-prod.yml application-prod.yml
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
