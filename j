#!/bin/sh

# compile
javac -d bin src/*.java

# remove old jar
rm JDec.jar

# create new jar (and move to top directory)
cd bin
jar cfm JDec.jar manifest.txt *.class
mv JDec.jar ../JDec.jar
cd ..

# run jar
java -jar JDec.jar
