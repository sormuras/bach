# Bach.java

---
@snap[north span-90]
## Overview
@snapend
@snap[midpoint span-90]
- Motivation
- Usage
- Features
- Project Model
- Outlook
@snapend

---
@snap[north span-90]
## Motivation
@snapend
@snap[midpoint span-90]
- Why doesn't the JDK provide a build tool?
- Why another build tool?
@snapend

---?image=doc/img/jdk-and-build-tools.svg&size=90% auto
@title[JDK and Build Tools]
@snap[north span-90]
## JDK and Build Tools
@snapend

+++?image=doc/img/jdk-and-build-tools-with-bach.svg&size=90% auto
@title[JDK, Bach.java, Tools]
@snap[north span-90]
## JDK and Build Tools
@snapend

+++
@snap[north span-90]
## JDK Tools 2019
@snapend
@snap[midpoint span-90]
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
@snapend

---
@snap[north span-90]
## Screenplays
@snapend
@snap[midpoint span-90]
- from `build.bat`
- over `build.jsh`
- to `https://bit.ly/bach-jsh`
@snapend

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
@snap[north span-90]
## Features
@snapend
@snap[midpoint span-90]
- Lightweight
- Java
- Modules
@snapend

---
@snap[north span-90]
## Project Model
@snapend
@snap[midpoint span-90]
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
@snapend

---
@snap[north span-90]
## Usage
@snapend
@snap[midpoint span-90]
- `jshell ...`
- `java -p ... -m de.sormuras.bach`
@snapend

---
@snap[north span-90]
## Outlook
@snapend
@snap[midpoint span-90]
@ul
- from `Bach.java` to `{JDK_HOME}/bin/jbuild[.exe]`?
- to `jbach`, `jbuild`, `javab`, ...
- create a JEP?
@ulend
@snapend
