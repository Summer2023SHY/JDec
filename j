#!/bin/sh

# compile
javac -d bin src/*.java

cd bin

# remove old jar
rm JDec.jar

# create new jar
jar cfm JDec.jar manifest.txt *.class

# move jar to top directory
mv JDec.jar ../JDec.jar
cd ..

# run jar
java -jar JDec.jar
