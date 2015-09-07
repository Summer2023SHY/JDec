#!/bin/sh
set -e
javac -d bin src/*.java
java -cp ./bin Liu2
