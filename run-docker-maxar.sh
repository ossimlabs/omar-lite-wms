#!/bin/sh

IMAGE=nexus-docker-private-hosted.ossim.io/omar-lite-wms
VERSION=latest

docker run -it --rm  -p 7777:8080 -e JAVA_OPTIONS="-Dmicronaut.server.context-path=/omar-wms" \
-v $OSSIM_DATA:$OSSIM_DATA \
-v $OSSIM_DATA:/data \
-v /Volumes/SeagateHD/data/elevationTiled:/Volumes/SeagateHD/data/elevationTiled \
-e OSSIM_PREFS_FILE=/Volumes/SeagateHD/data/elevationTiled/ossim_preferences.linux.gegd \
$IMAGE:$VERSION
