#!/bin/sh
sbt universal:package-zip-tarball
docker build -t scapig-developer-hub .
docker tag scapig-developer-hub scapig/scapig-developer-hub
docker push scapig/scapig-developer-hub
