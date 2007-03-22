CP=$JAVA_HOME/../Classes/classes.jar
jikes -classpath $CP -d build/classes src/com/pingdynasty/midi/*.java
