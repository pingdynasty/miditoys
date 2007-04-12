CP=$JAVA_HOME/../Classes/classes.jar:build/classes:src/properties:lib/plumstoneserv.jar:lib/jinput.jar:lib/jutils.jar:lib/jvaptools.jar:lib/jVSTwRapper_bin.jar:lib/jVSTsYstem_bin.jar
javac -classpath $CP -d build/classes `find src/com -name '*.java'` && \
java -Djava.library.path=lib/native/macosx -classpath $CP $@
