CP=$JAVA_HOME/../Classes/classes.jar:build/classes:src/properties
for nm in lib/*.jar
do CP=$CP:$nm
done
javac -classpath $CP -d build/classes src/com/pingdynasty/pong/*.java && \
java -ea -Djava.library.path=lib/native/macosx -classpath $CP com.pingdynasty.pong.Pong
