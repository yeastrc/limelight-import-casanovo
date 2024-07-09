FROM amazoncorretto:11.0.17

ADD build/libs/casanovoToLimelightXML.jar  /usr/local/bin/casanovoToLimelightXML.jar
ADD entrypoint.sh /usr/local/bin/entrypoint.sh
ADD casanovoToLimelightXML /usr/local/bin/casanovoToLimelightXML

RUN chmod 755 /usr/local/bin/entrypoint.sh && chmod 755 /usr/local/bin/casanovoToLimelightXML
RUN yum update -y && yum install -y procps

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
CMD []
