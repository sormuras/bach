# Bach - Java Shell Builder - Builds (on(ly)) Modules

> "The tools we use have a profound (and devious!) influence on our thinking habits, and, therefore, on our thinking abilities."
>
> [E. W. Dijkstra, 18 June 1975](https://www.cs.virginia.edu/~evans/cs655/readings/ewd498.html)

Bach is a tool that orchestrates [JDK tools] for building modular Java projects.

If you are eager to try out Bach and already set [JDK 22] or higher up, these simple steps work most of the time:

```shell
mkdir example && cd example
jshell
/open https://install.bach.run
java @bach jar --version
java @bach jcmd -l
java @bach https://src.bach.run/Hi.java Lo
```

For detailed installation instructions please see the [installing](doc/installing.md) document.

# be free - have fun

[![jdk22](https://img.shields.io/badge/JDK-22-blue.svg)](https://jdk.java.net)
![experimental](https://img.shields.io/badge/API-experimental-yellow.svg)
[![jsb](https://upload.wikimedia.org/wikipedia/commons/thumb/6/65/Bachsiegel.svg/220px-Bachsiegel.svg.png)](https://wikipedia.org/wiki/Johann_Sebastian_Bach)

[JDK tools]: https://docs.oracle.com/en/java/javase/22/docs/specs/man/index.html
[JDK 22]: https://jdk.java.net/22
