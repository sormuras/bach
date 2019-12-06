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

## JDK Tools and Build Tools

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

## JDK Tools 2019

- **1** `javac` Compiler for the Java programming language
- **1** `javap` Class file disassembler
- **1** `javadoc` API documentation generator
- **1** `java` Launcher for Java applications
- **1** `jar` Java Archive (JAR) file manager
- **8** `jdeps` Class dependency analyzer
- **8** `jdeprscan` Deprecated API use finder
- **9** `jlink` Custom runtime image assembler
- **9** `jmod` JMOD file manager
- **14** `jpackage` Package self-contained Java applications

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

## Screenplays

- from `build.bat`
- over `build.jsh`
- to `https://bit.ly/bach-jsh`

Note:
- From source to module in 6 steps
- Created with DOS batch script
- Recorded with https://blog.bahraniapps.com/gifcam-6-0/

+++?image=doc/screenplay/demo-1-build.gif&size=auto 90%
@snap[north-east span-40]
#### Demo 1 `build.bat`
@snapend
@snap[east span-40]
@ul
- Step 1: Declare module
- Step 2: `build.bat`
- Step 3: Tree sources
- Step 4: Build!
- Step 5: Tree binaries
- Step 6: Describe module
@ulend
@snapend

Note:
- From source to module in 6 steps
- Using DOS `.bat` syntax

+++?image=doc/screenplay/demo-2-jshell.gif&size=auto 90%

Note:
#### Demo 2 `jshell build.jsh`

+++?image=doc/screenplay/demo-3-bach.gif&size=auto 90%

Note:
#### Demo 3 `jshell https://bit.ly/bach-jsh`

---

## Outlook

- from `Bach.java` to `{JDK_HOME}/bin/jbuild[.exe]`?
- to `jbach`, `jbuild`, `javab`, ...
- create a JEP?
