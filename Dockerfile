FROM openjdk:14-alpine
COPY build/libs/omar-lite-wms-*-all.jar omar-lite-wms.jar
EXPOSE 8080
CMD ["java", "-Dcom.sun.management.jmxremote", "-Xmx128m", "-jar", "omar-lite-wms.jar"]