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
docker tag scapig-developer-hub scapig/scapig-developer-hub:VERSION
docker login
docker push scapig/scapig-developer-hub:VERSION
``

## Running
``
docker run -p9017:9017 -d scapig/scapig-developer-hub:VERSION
``
