FROM openjdk:8

COPY target/universal/tapi-developer-hub-*.tgz .
COPY start-docker.sh .
RUN chmod +x start-docker.sh
RUN tar xvf tapi-developer-hub-*.tgz

EXPOSE 8010