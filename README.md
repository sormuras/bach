# Bach - Java Shell Builder - Builds (on(ly)) Modules

> "The tools we use have a profound (and devious!) influence on our thinking habits, and, therefore, on our thinking abilities."
>
> [E. W. Dijkstra, 18 June 1975](https://www.cs.virginia.edu/~evans/cs655/readings/ewd498.html)

Bach is a Java Shell-like that orchestrates [JDK tools] for building modular Java projects.

## Prelude

Install Bach's basic tool framework using its sources via `git` and Java 22 or higher.

First time:
```shell
mkdir myproject
cd myproject
git init
git submodule add https://github.com/sormuras/run.bach .bach/src/run.bach/run/bach
echo .bach/src/run.bach/run/bach/internal/RunTool.java > .bach/run
```

Subsequent times:
```shell
cd myproject
git submodule update --remote --recursive
```

Run time:
```shell
java @.bach/run jar --version
java @.bach/run java --version
java @.bach/run https://raw.githubusercontent.com/sormuras/hello/main/Hello.java World
java @.bach/run https://github.com/sormuras/hello/releases/download/1-M3/hello-1-M3.jar World
```
Consult the following manual pages for details of `git` and `java` tools:
- [git init](https://git-scm.com/docs/git-init) - Create an empty Git repository or reinitialize an existing one
- [git submodule](https://git-scm.com/docs/git-submodule) - Initialize, update or inspect submodules
- [java @file](https://docs.oracle.com/en/java/javase/22/docs/specs/man/java.html#java-command-line-argument-files) - Java Command-Line Argument Files

# be free - have fun

[![jdk22](https://img.shields.io/badge/JDK-22-blue.svg)](https://jdk.java.net)
![experimental](https://img.shields.io/badge/API-experimental-yellow.svg)
[![jsb](https://upload.wikimedia.org/wikipedia/commons/thumb/6/65/Bachsiegel.svg/220px-Bachsiegel.svg.png)](https://wikipedia.org/wiki/Johann_Sebastian_Bach)

[JDK tools]: https://docs.oracle.com/en/java/javase/22/docs/specs/man/index.html
