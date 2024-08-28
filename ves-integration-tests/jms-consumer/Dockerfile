FROM openjdk:8-jre-alpine

COPY target/jms-consumer-*.jar .
COPY src/main/resources/consumer.properties .

CMD java -jar jms-consumer-*.jar