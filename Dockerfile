FROM openjdk:8

COPY target/universal/scapig-developer-hub-*.tgz .
COPY start-docker.sh .
RUN chmod +x start-docker.sh
RUN tar xvf scapig-developer-hub-*.tgz
EXPOSE 8010

CMD ["sh", "start-docker.sh"]