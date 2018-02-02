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

## Publishing
``
docker tag scapig-developer-hub scapig/scapig-developer-hub
docker login
docker push scapig/scapig-developer-hub
``

## Running
``
docker run -p9020:9020 -d scapig/scapig-developer-hub
``
