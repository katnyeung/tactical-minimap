FROM openjdk:8-jdk-alpine
VOLUME /tmp
ADD limap-0.0.1-SNAPSHOT.jar limap.jar
ENTRYPOINT ["java","-XX:+UseSerialGC","-Djava.security.egd=file:/dev/./urandom","-jar","/limap.jar"]
ENV LANG en_US.UTF-8
RUN apk add --update ttf-dejavu fontconfig && rm -rf /var/cache/apk/*
