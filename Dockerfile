# Track the Amazon Corretto 11 major-version tag so each rebuild picks up the latest patch-level
# (security) release automatically, instead of running a non-reproducible "yum update" at build time.
FROM amazoncorretto:11

COPY build/libs/casanovoToLimelightXML.jar /usr/local/bin/casanovoToLimelightXML.jar
COPY entrypoint.sh /usr/local/bin/entrypoint.sh
COPY casanovoToLimelightXML /usr/local/bin/casanovoToLimelightXML

RUN chmod 755 /usr/local/bin/entrypoint.sh /usr/local/bin/casanovoToLimelightXML

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
CMD []
