#!/bin/sh
set -e
javac *.java
t -c -d $1&
java AutomataGUI
