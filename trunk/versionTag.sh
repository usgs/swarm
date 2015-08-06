#!/bin/sh

TAG="Swarm-2.3.4"

for i in  Earthworm Math Net Plot Util Winston Swarm 
do
svn copy http://avosouth.wr.usgs.gov/vhpsvn/${i}/trunk \
           http://avosouth.wr.usgs.gov/vhpsvn/${i}/tags/$TAG \
      -m "Tagging the $TAG release of the swarm."
done
