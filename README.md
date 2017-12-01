## tapi-developer-hub

## Building
``
sbt clean test it:test component:test
``

## Packaging
``
sbt universal:package-zip-tarball
docker build -t tapi-developer-hub .
``

## Running
``
docker run -p8010:8010 -i -a stdin -a stdout -a stderr tapi-developer-hub sh start-docker.sh
``