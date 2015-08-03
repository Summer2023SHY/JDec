#!/bin/sh
set -e
javac -d bin src/*.java
t -c -v -d&
java -cp ./bin JDec
