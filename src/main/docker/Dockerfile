FROM openjdk:8-jdk-alpine
VOLUME /tmp
ADD limap-0.0.1-SNAPSHOT.jar limap.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/limap.jar"]