CP=$JAVA_HOME/../Classes/classes.jar:build/classes:src/properties:lib/plumstoneserv.jar:lib/jinput.jar
javac -classpath $CP -d build/classes src/com/pingdynasty/*/*.java && \
java -Djava.library.path=lib/native/macosx -classpath $CP com.pingdynasty.pong.Pong
