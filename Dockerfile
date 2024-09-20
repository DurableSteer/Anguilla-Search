FROM openjdk:17-alpine 

ENV JAVA_OPTS="-Xmx256m -Xms256m -Djava.awt.headless=true"
ARG JAR_FILE="anguillasearch-1.0.0-SNAPSHOT.jar"
WORKDIR /opt/anguillasearch
COPY ${JAR_FILE} app.jar
RUN apk add --no-cache fontconfig ttf-dejavu

VOLUME /opt/anguillasearch/libs
VOLUME /opt/anguillasearch/logs

ENTRYPOINT ["java", "-jar", "/opt/anguillasearch/app.jar"]

