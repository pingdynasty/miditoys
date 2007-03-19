CP=$JAVA_HOME/../Classes/classes.jar
jikes -classpath $CP -d build/classes src/com/pingdynasty/midi/WalkingBass.java && \
java -classpath lib/plumstoneserv.jar:build/classes com.pingdynasty.midi.WalkingBass
