CP=$JAVA_HOME/../Classes/classes.jar:lib/jinput.jar:build/classes
jikes -classpath $CP -d build/classes src/com/pingdynasty/*/*.java
