FROM openjdk:17-jdk-slim

WORKDIR /slithers-smashers-back
COPY ./target/slithers-smashers-back-0.0.1-SNAPSHOT.jar /slithers-smashers-back

EXPOSE 8080

CMD ["java", "-jar", "/slithers-smashers-back/slithers-smashers-back-0.0.1-SNAPSHOT.jar"]