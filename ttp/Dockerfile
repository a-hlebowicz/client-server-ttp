FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
COPY shared ./shared
COPY ttp ./ttp
COPY server ./server
COPY client ./client


RUN mvn package -pl ttp -am -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /build/ttp/target/*.jar app.jar

EXPOSE 5000
ENTRYPOINT ["java", "-jar", "app.jar"]
