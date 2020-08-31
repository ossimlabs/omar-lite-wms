#!/bin/sh

docker run -it --rm  -p 7777:8080 -v $OSSIM_DATA:$OSSIM_DATA -v $OSSIM_DATA:/data nexus-docker-public-hosted.ossim.io/omar-lite-wms:latest
