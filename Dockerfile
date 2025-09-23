FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY build/libs/*-all.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]