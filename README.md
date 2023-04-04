# JDec v1.0

[![CI](https://github.com/Summer2023SHY/Automata/actions/workflows/ci.yml/badge.svg)](https://github.com/Summer2023SHY/Automata/actions/workflows/ci.yml)

## Downloading

```bash
git clone https://github.com/Summer2023SHY/Automata.git
```

## Updating

```bash
git pull
```

## Running

```bash
javac -d bin src/*.java
java -cp ./bin JDec
```

## Testing

```bash
javac -d bin src/*.java
java -cp ./bin TestAutomata
```

(NOTE: `-v` flag is used for verbose mode, `-c` flag is used to enable colored output, `-d` flag is used to enable diff-like output)

## Creating and Opening Javadocs

```bash
javadoc -private -d docs/ src/*.java
```

Then open `docs/index.html`

## Dependencies

- Java 1.7
- GraphViz (Tested using v2.14.1 from http://www.ryandesign.com/graphviz/).  
  The directory of the installed program must be added to the PATH variable.
- X11 (Can be downloaded from http://xquartz.macosforge.org/landing/)  
  NOTE: Upgrading to a new release of OSX may actually uninstall this application, which will require you to reinstall it.
