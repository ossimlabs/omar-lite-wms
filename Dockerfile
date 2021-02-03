#FROM openjdk:14-alpine
FROM nexus-docker-public-hosted.ossim.io/openjdk11:alpine-slim
COPY build/libs/omar-lite-wms-*-all.jar omar-lite-wms.jar
EXPOSE 8080
CMD ["java", "-Dcom.sun.management.jmxremote", "-Xmx128m", "-jar", "omar-lite-wms.jar"]


