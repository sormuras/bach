# Bach - Java Shell Builder
 
[![jdk16](https://img.shields.io/badge/JDK-16-blue.svg)](https://jdk.java.net)
[![experimental](https://img.shields.io/badge/API-experimental-yellow.svg)](https://github.com/sormuras/bach)

Build modular Java projects with [JDK Foundation Tools].

## Boot Bach into a JShell Session

- Install JDK 16 (or higher) from https://jdk.java.net
- Open a command shell in an empty directory
- Enter `jshell https://bit.ly/bach-main-boot` to launch JShell booting Bach - this is a short-cut for `jshell` and `/load https://github.com/sormuras/bach/raw/main/boot`
- Type `/list` to display the set of pre-defined API methods
- Try `find("**.jar")`, `describeModules("java.logging")`, `listSystemModules()`, and other methods
- Exit JShell via `/exit`

# be free - have fun

[![jsb](https://upload.wikimedia.org/wikipedia/commons/thumb/6/65/Bachsiegel.svg/220px-Bachsiegel.svg.png)](https://wikipedia.org/wiki/Johann_Sebastian_Bach)

[JDK Foundation Tools]: https://docs.oracle.com/en/java/javase/15/docs/specs/man
