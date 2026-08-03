FROM openjdk:17-jdk-slim
VOLUME /tmp
ENV SPRING_PROFILES_ACTIVE=docker
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]