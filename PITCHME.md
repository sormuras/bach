@snap[midpoint span-100]
# Bach.java
[github.com/sormuras/bach](https://github.com/sormuras/bach)
@snapend
@snap[south span-100 text-07 text-blue]
😉 Christian Stein &nbsp; 🐦[@sormuras](https://twitter.com/sormuras) &nbsp; 📰[sormuras.github.io](https://sormuras.github.io)
@snapend

+++?image=https://raw.githubusercontent.com/sormuras/sormuras.github.io/master/asset/img/avatar-sormuras-1000-1000.jpg&position=left&size=55% 100%
@snap[north span-90]
## About me
@snapend
@snap[midpoint span-90]
<a href="https://www.micromata.de"><img src="https://github.com/sormuras/testing-in-the-modular-world/raw/master/img/micromata-logo-horizontal.png" height="120" /></a>
</br>
<a href="https://junit.org/junit5"><img src="https://github.com/sormuras/testing-in-the-modular-world/raw/master/img/junit5-logo.png" height="60" /></a>
&nbsp;
<a href="https://maven.apache.org"><img src="https://github.com/sormuras/testing-in-the-modular-world/raw/master/img/maven-logo-black-on-white.png" height="60" /></a>
@snapend

---
@snap[north span-90]
## Motivation
@snapend
@snap[midpoint span-90]
@ul[list-no-bullets](false)
- **Motivation**
- @css[text-gray](Demo)
- @css[text-gray](Features)
- @css[text-gray](Model)
- @css[text-gray](Outlook)
@ulend
@snapend

+++
@snap[north span-90]
### Why...
@snapend
@snap[midpoint span-90]
- ... doesn't the JDK provide a build tool?
- ... another build tool?
@snapend
@snap[south span-100 text-07 text-blue]
Bach.java - Motivation
@snapend

+++?image=doc/img/jdk-and-build-tools.svg&size=90% auto
@title[JDK and Build Tools]
@snap[north span-90]
### JDK and Build Tools
@snapend
@snap[south span-100 text-07 text-blue]
Bach.java - Motivation
@snapend

+++?image=doc/img/jdk-and-build-tools-with-bach.svg&size=90% auto
@title[JDK, Bach.java, Tools]
@snap[north span-90]
### JDK and Build Tools
@snapend
@snap[south span-100 text-07 text-blue]
Bach.java - Motivation
@snapend

+++
@snap[north span-90]
### JDK Tools 2019
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
@snap[south span-100 text-07 text-blue]
Bach.java - Motivation
@snapend

+++
@snap[north span-90]
### No tricks
@snapend
@snap[midpoint span-90]
@ul[text-08](false)
- No .xml, .yaml, ... .z??
- No .groovy, .kts, ...
- No daemons, no cache services, ...
@ulend
@snapend
@snap[south span-100 text-07 text-blue]
Bach.java - Motivation
@snapend

---
@snap[north span-90]
## Demo
@snapend
@snap[midpoint span-90]
@ul[list-no-bullets](false)
- Motivation ✔
- **Demo**
- @css[text-gray](Features)
- @css[text-gray](Model)
- @css[text-gray](Outlook)
@ulend
@snapend

+++
@snap[north span-90]
### Live or GIF
@snapend
@snap[midpoint span-90]
1. `build.bat`
1. `build.jsh`
1. `jshell https://bit.ly/bach-jsh`
@snapend
@snap[south span-100 text-07 text-blue]
Bach.java - Demo
@snapend

Note:
- From source to module in 6 steps
- Created with DOS batch script
- Recorded with https://blog.bahraniapps.com/gifcam-6-0/

+++?image=doc/screenplay/demo-1-build.gif&size=auto 90%
@snap[north-east]
### Demo 1
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
@snap[south span-100 text-07 text-blue]
Bach.java - Demo 1 - build.bat
@snapend

Note:
- From source to module in 6 steps
- Using DOS `.bat` syntax

+++?image=doc/screenplay/demo-2-jshell.gif&size=auto 90%
@snap[north-east]
### Demo 2
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
@snap[south span-100 text-07 text-blue]
Bach.java - Demo 2 - jshell build.jsh
@snapend

Note:
- From source to module in 6 steps
- Using JShell syntax stored in `.jsh` file

+++?image=doc/screenplay/demo-3-bach.gif&size=auto 90%
@snap[north-east]
### Demo 3
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
@snap[south span-100 text-07 text-blue]
Bach.java - Demo 3 - jshell https://bit.ly/bach-jsh
@snapend

Note:
- From source to module in 6 steps
- Using no extra script file

---
@snap[north span-90]
## Features
@snapend
@snap[midpoint span-90]
@ul[list-no-bullets](false)
- Motivation ✔
- Demo ✔
- **Features**
- @css[text-gray](Model)
- @css[text-gray](Outlook)
@ulend
@snapend

+++
@snap[north span-90]
### Concepts
@snapend
@snap[midpoint span-90]
- Lightweight
- Java
- Modules
@snapend
@snap[south span-100 text-07 text-blue]
Bach.java - Features
@snapend

+++
@snap[north span-90]
### Uniques
@snapend
@snap[midpoint span-90 text-06]
- **zero installation** required (besides JDK 11+, using `jshell`)
- **zero configuration** required (conventions and information gathered from `module-info.java` files)
- **b-y-o-b** program using plain old Java (`src/bach/Build.java`)
- **3rd-party modules** in plain sight (single `lib/` directory)
- **compilation** is compile (`javac`) and package (`jar`) as an atomic step
- **multi-module** compilation in a single pass (`--module-source-path`, `${PROJECT.NAME}-javadoc.jar`)
- **multi-release** modules are supported (`java-7`, `java-8`, ..., `java-11`, ..., `java-N`)
- **automated checks** (aka testing) built-in (`test(${MODULE})`,`junit`)
- **deployment** support (via `${MODULE}-sources.jar` and consumer `pom.xml` per module)
@snapend
@snap[south span-100 text-07 text-blue]
Bach.java - Features
@snapend

---
@snap[north span-90]
## Model
@snapend
@snap[midpoint span-90]
@ul[list-no-bullets](false)
- Motivation ✔
- Demo ✔
- Features ✔
- **Model**
- @css[text-gray](Outlook)
@ulend
@snapend

+++
@snap[north span-90]
### Types
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
@ulend
@snapend
@snap[south span-100 text-07 text-blue]
Bach.java - Model
@snapend

---
@snap[north span-90]
## Outlook
@snapend
@snap[midpoint span-90]
@ul[list-no-bullets](false)
- Motivation ✔
- Demo ✔
- Features ✔
- Model ✔
- **Outlook**
@ulend
@snapend

+++
@snap[north span-90]
### JEP
@snapend
@snap[midpoint span-90]
@ul
- From `Bach.java`...
- to `{JDK_HOME}/bin/jbuild[.exe]`?
- Or `jbach`, `jbuild`, `javab`,...
- Create a JEP?
@ulend
@snapend
@snap[south span-100 text-07 text-blue]
Bach.java - Outlook
@snapend

---
@snap[north span-90]
# Thanks.
#### Happy building!
@snapend
@snap[midpoint span-90]
@ul

@ulend
@snapend
@snap[south span-100 text-07 text-blue]
😉 Christian Stein &nbsp; 🐦[@sormuras](https://twitter.com/sormuras) &nbsp; 📰[sormuras.github.io](https://sormuras.github.io)
@snapend

---
@snap[north span-90]
## Backup
@snapend
@snap[midpoint span-90]
@ul[list-no-bullets](false)
- Motivation ✔
- Demo ✔
- Features ✔
- Model ✔
- Outlook ✔
- **Backup**
@ulend
@snapend

+++
@snap[north span-90]
### Demo: Bach + JavaFX
@snapend
@snap[midpoint span-90]
- https://github.com/sormuras/bach-javafx
@snapend
@snap[south span-100 text-07 text-blue]
Bach.java - Backup
@snapend
