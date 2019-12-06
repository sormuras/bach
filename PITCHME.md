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

---?image=doc/img/jdk-and-build-tools.svg&size=auto 90%
## JDK and Build Tools

---

## JDK Tools 2019

@ul[text-08](false)
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
@ulend

---

## Features

- Lightweight
- Java
- Modules

---

## Project Model

@ul[text-08](false)
- Package `de.sormuras.bach.project`
- `Project`
  - `Name`
  - `Version`
- `Structure`
  - `Folder`
  - `Realm`
  - `Unit`
    - `Source`
  - `Library`
- `Deployment`
@ulend

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
@snap[north-east]
## Demo 1
@snapend

@snap[east]
@ol[text-08]
1. Declare module
1. `build.bat`
1. Tree sources
1. Build!
1. Tree binaries
1. Describe module
@olend
@snapend

Note:
- From source to module in 6 steps
- Using DOS `.bat` syntax

+++?image=doc/screenplay/demo-2-jshell.gif&size=auto 90%
@snap[north-east]
## Demo 2
@snapend

@snap[east]
@ol[text-08]
1. Declare module
1. `build.jsh`
1. Tree sources
1. Build!
1. Tree binaries
1. Describe module
@olend
@snapend

Note:
- From source to module in 6 steps
- Using JShell syntax stored in `.jsh` file

+++?image=doc/screenplay/demo-3-bach.gif&size=auto 90%
@snap[north-east]
## Demo 3
@snapend

@snap[east]
@ol[text-08]
1. Declare module
1. *NOOP*
1. Tree sources
1. Build!
1. Tree binaries
1. Describe module
@olend
@snapend

Note:
- From source to module in 6 steps
- Using no extra script file

---

## Outlook

@ul
- from `Bach.java` to `{JDK_HOME}/bin/jbuild[.exe]`?
- to `jbach`, `jbuild`, `javab`, ...
- create a JEP?
@ulend
