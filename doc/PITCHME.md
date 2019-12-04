# Bach.java

---

## Overview

- Motivation
- Features
- Project Model
- Usage
- Outlook

---

## Motivation

- Why doesn't the JDK provide a build tool?
- Why another build tool?

---

## JDK Tools 2019

```text
JDK Foundation Tools                    Multi-Purpose Build Tools
 |                                                  Maven
 |                                Ant(+Ivy)          | Gradle
 |                 Bach.java       |                 |  | Bazel Buildr
 |  Scripts         |              |                 | Buck| sbt |
 |   |              |              |                 |  |  |  |  |
 +---+--------------+--------------+-----------------+--+--+--+--+-----
 |
  \ javac, javap, javadoc, java, jar, jlink, jmod, jdeps, and jdeprscan
```

---

## JDK Tools 2020+

- **14** `jpackage` https://openjdk.java.net/jeps/343
- **?** `jbach`, `jbuild`, `javab`, ... JEP ?

---

## Features

- Lightweight
- Java
- Modules

---

## Project Model

- Package `de.sormuras.bach.project`
- Project
  - Name
  - Version
- Structure
  - Folder
  - Realm
  - Unit
    - Source
  - Library
- Deployment

---

## Usage

- `jshell ...`
- `java -p ... -m de.sormuras.bach`

---

## Outlook

- from `Bach.java` to `{JDK_HOME}/bin/jbuild[.exe]`?
