CP=$JAVA_HOME/../Classes/classes.jar
javac -classpath $CP -d build/classes src/com/pingdynasty/midi/*.java && \
java -classpath lib/plumstoneserv.jar:build/classes:src/properties com.pingdynasty.midi.Keyboard
