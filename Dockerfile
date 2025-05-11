FROM openjdk:21-jdk AS build
WORKDIR /app
COPY pom.xml .
COPY src src

COPY mvnw .
COPY .mvn .mvn

RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests

# Stage 2: Use OpenJDK 19 as runtime (if you really need 19 here)
FROM openjdk:19-jdk
VOLUME /tmp
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
EXPOSE 8080
