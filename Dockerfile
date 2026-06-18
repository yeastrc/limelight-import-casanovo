# Pinned Amazon Corretto 11 base image. Bump this tag deliberately to pick up security updates,
# rather than running a non-reproducible "yum update" at build time.
FROM amazoncorretto:11.0.31

COPY build/libs/casanovoToLimelightXML.jar /usr/local/bin/casanovoToLimelightXML.jar
COPY entrypoint.sh /usr/local/bin/entrypoint.sh
COPY casanovoToLimelightXML /usr/local/bin/casanovoToLimelightXML

RUN chmod 755 /usr/local/bin/entrypoint.sh /usr/local/bin/casanovoToLimelightXML

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
CMD []
