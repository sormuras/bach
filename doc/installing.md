# Installing Bach

This document describes common ways to install Bach.

Bach is usually installed as source code relative to each project's root directory.
The default directory path is `.bach/src/run/bach`.

## Prerequisite

- JDK 25 or higher

## Install Bach using JShell

Install Bach using `jshell` by running the snippets from the default installation script.
The https://install.bach.run URL forwards to the _"install default version of Bach into `.bach` directory of the current working directory"_ [Java Shell](../src/bach.run/install.jshell) script.

```shell
mkdir example && cd example
jshell
/open https://install.bach.run
```

Above's commands are a shortcut for the following Java Shell commands and snippets.

```shell
mkdir example && cd example
jshell
/open https://src.bach.run/Bach.java
Bach.init()
/exit
```

Consult `Bach.java`'s source and documentation for customizing the installation process.

## Install Bach using Git

Install Bach using `git` and create `java`'s argument file manually.

First time:
```shell
mkdir example && cd example
git init
git submodule add https://github.com/sormuras/run.bach .bach/src/run/bach
echo .bach/src/run/bach/Main.java > bach
```
Consult the following manual pages for details of `git` and `java` tools:
- [git init](https://git-scm.com/docs/git-init) - Create an empty Git repository or reinitialize an existing one
- [git submodule](https://git-scm.com/docs/git-submodule) - Initialize, update or inspect submodules
- [java @file](https://docs.oracle.com/en/java/javase/25/docs/specs/man/java.html#java-command-line-argument-files) - Java Command-Line Argument Files

Subsequent times:
```shell
cd example
git submodule update --remote --recursive
```

## Play with Bach

Running a tool via the `ToolProvider` SPI.

- `java @bach jar --version`

Running a tool via the `ProcessBuilder` API.

- `java @bach jcmd -l`

Running a tool via the `ToolInstaller` API.

- `java @bach https://src.bach.run/Hi.java Lo`
