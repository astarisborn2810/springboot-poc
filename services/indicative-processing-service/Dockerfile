FROM eclipse-temurin:24-jre-alpine

ARG JAR_FILE=target/*.jar
WORKDIR /opt/pearl

RUN addgroup -S pearl && adduser -S pearl -G pearl
COPY ${JAR_FILE} app.jar

USER pearl
EXPOSE 8082

ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-XX:+UseG1GC","-jar","/opt/pearl/app.jar"]
