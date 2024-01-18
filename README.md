# <img src="logo.png" width="48"> JDec v2.0.0

[![CI](https://github.com/Summer2023SHY/JDec/actions/workflows/ci.yml/badge.svg)](https://github.com/Summer2023SHY/JDec/actions/workflows/ci.yml) [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)

JDec is a Java application for decentralized control.

## Features

- Creating, saving, and loading automata
- Generating $\mathcal{U}$ structures of finite languages
- Determining inference observability for finite languages (up to self-loops)

## Requirements

- Java 17 or later
- Apache Maven (if you are building from source)

## Downloading and Running

Prebuilt JAR files for JDec are available in the [releases](https://github.com/Summer2023SHY/JDec/releases) page.
Download and double-click the JAR file to launch JDec.

## User Guide

A [user guide](https://github.com/Summer2023SHY/JDec/wiki/User_Guide) is available in our [Wiki](https://github.com/Summer2023SHY/JDec/wiki/).

## Developing

### Downloading Source

```bash
git clone https://github.com/Summer2023SHY/JDec.git
```

### Running from Source

```bash
mvn compile exec:java
```

### Testing

```bash
mvn test
```

### Creating and Opening Javadocs

```bash
mvn javadoc:aggregate
```

Then open `target/site/apidocs/index.html`

## License

[The MIT License](./LICENSE)
