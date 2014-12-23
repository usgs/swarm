#!/bin/sh

TAG="Swarm_2.3.5"
BASE="http://avosouth.wr.usgs.gov/vhpsvn"
tag() {
        svn copy ${BASE}/${1}/trunk ${BASE}/${1}/tags/$TAG -m "Tagging $TAG"
}

tag Swarm
tag Earthworm
tag Math
tag Net
tag Plot
tag USGS
tag Util
tag Winston
