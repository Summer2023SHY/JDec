#!/bin/sh
set -e
javac *.java
t -c -d&
java AutomataGUI
