## scapig-developer-hub

## Building
``
sbt clean test it:test component:test
``

## Packaging
``
sbt universal:package-zip-tarball
docker build -t scapig-developer-hub .
``

## Running
``
docker run -p8010:8010 -i -a stdin -a stdout -a stderr scapig-developer-hub sh start-docker.sh
``