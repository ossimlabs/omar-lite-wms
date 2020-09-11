#!/bin/sh

#docker run -it --rm  -p 7777:8080 -v $OSSIM_DATA:$OSSIM_DATA -v $OSSIM_DATA:/data --entrypoint sh nexus-docker-public-hosted.ossim.io/omar-lite-wms 
docker run -it --rm  -p 7777:8080 -v $OSSIM_DATA:$OSSIM_DATA -v $OSSIM_DATA:/data --entrypoint sh omar-lite-wms 
