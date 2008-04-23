CP=$JAVA_HOME/../Classes/classes.jar:build/classes:src/properties
for nm in lib/*.jar
do CP=$CP:$nm
done
javac -classpath $CP -d build/classes `find src/com/pingdynasty/midi -name '*.java'` && \
java -Djava.library.path=lib/native/macosx -classpath $CP $@
