CP=$JAVA_HOME/../Classes/classes.jar:build/classes:src/properties:lib/plumstoneserv.jar
jikes -classpath $CP -d build/classes src/com/pingdynasty/*/*.java && \
java -classpath $CP com.pingdynasty.pong.Pong
