FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Prende il file specifico e lo rinomina in 'app.jar' internamente
COPY target/order-service-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]