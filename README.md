# Automata

<b>Downloading:</b>

    git clone https://github.com/micahstairs/Automata.git
    
<b>Updating:</b>

    git pull

<b>Running:</b>

    javac -d bin src/*.java
    java -cp ./bin JDec

<b>Testing:</b>

    javac -d bin src/*.java
    java -cp ./bin TestAutomata (NOTE: -v flag is used for verbose mode, -c flag is used to enable colored output, -d flag is used to enable diff-like output)
   
<b>Creating and Opening Javadocs:</b>

    javadoc -private -d docs/ src/*.java
    open docs/index.html

<b>Dependencies:</b>

<ul>
<li>GraphViz (Tested using v2.14.1 from http://www.ryandesign.com/graphviz/). The directory of the installed program must be added to the PATH variable.</li>
<li>X11 (Can be downloaded from http://xquartz.macosforge.org/landing/) NOTE: Upgrading to a new release of OSX may actually uninstall this application, which will require you to reinstall it.</li>
</ul>
