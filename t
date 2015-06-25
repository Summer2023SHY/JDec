#!/bin/sh
set -e
javac *.java
java TestAutomata $1 $2 $3
