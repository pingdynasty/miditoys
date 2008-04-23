#!/bin/sh
javac  -d build/classes `find src/com/pingdynasty/phosphoros -name '*.java'` && \
java -cp build/classes com.pingdynasty.phosphoros.LiveCam
