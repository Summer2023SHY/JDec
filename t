#!/bin/sh
set -e
javac -d bin src/*.java
java -cp ./bin TestAutomata $1 $2 $3
