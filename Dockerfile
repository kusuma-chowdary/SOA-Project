FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
ARG APP_MODULE
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY discovery-server/pom.xml discovery-server/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY book-service/pom.xml book-service/pom.xml
COPY fine-service/pom.xml fine-service/pom.xml
COPY borrow-service/pom.xml borrow-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY common/src common/src
COPY discovery-server/src discovery-server/src
COPY auth-service/src auth-service/src
COPY book-service/src book-service/src
COPY fine-service/src fine-service/src
COPY borrow-service/src borrow-service/src
COPY api-gateway/src api-gateway/src
RUN mvn -B -pl ${APP_MODULE} -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
ARG APP_MODULE
COPY --from=build /workspace/${APP_MODULE}/target/${APP_MODULE}-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
