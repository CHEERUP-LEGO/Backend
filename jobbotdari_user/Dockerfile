# jobbotdari-user/Dockerfile
FROM gradle:8.12.1-jdk21 AS builder
WORKDIR /home/gradle/project
COPY --chown=gradle:gradle . .
RUN gradle build --no-daemon -x test

FROM openjdk:21-jdk-slim

# AWS Credentials
ARG ACCESS_KEY
ARG SECRET_KEY

ENV ACCESS_KEY=${ACCESS_KEY}
ENV SECRET_KEY=${SECRET_KEY}

VOLUME /tmp
COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar", "--spring.profiles.active=dev"]