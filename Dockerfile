FROM amazoncorretto:11-alpine-jdk

COPY build/libs/casanovoToLimelightXML.jar  /usr/local/bin/casanovoToLimelightXML.jar

ENTRYPOINT ["java", "-jar", "/usr/local/bin/casanovoToLimelightXML.jar"]