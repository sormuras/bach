# Bach.java - Java Shell Builder
 
[![jdk11](https://img.shields.io/badge/jdk-11-blue.svg)](http://jdk.java.net/11/)
[![travis](https://travis-ci.org/sormuras/bach.svg?branch=master)](https://travis-ci.org/sormuras/bach)
[![experimental](https://img.shields.io/badge/api-experimental-yellow.svg)](https://jitpack.io/com/github/sormuras/bach/master-SNAPSHOT/javadoc/)

Use Java source (in [jshell]) to build your modular Java project.

```text
    ___      ___      ___      ___
   /\  \    /\  \    /\  \    /\__\
  /::\  \  /::\  \  /::\  \  /:/__/_
 /::\:\__\/::\:\__\/:/\:\__\/::\/\__\
 \:\::/  /\/\::/  /\:\ \/__/\/\::/  /
  \::/  /   /:/  /  \:\__\    /:/  /
   \/__/    \/__/    \/__/    \/__/.java
```

> No need to be a maven to be able to use a build tool - [forax/pro](https://github.com/forax/pro)

:scroll:Fast-forward to [install-jdk.sh](#install-jdksh) section.

:fast_forward: Side-step to [make-java](https://github.com/sormuras/make-java) project.

## Execute Bach.java on-the-fly

This section will help you get started with `Bach.jsh` used as a remote `load-file` of [jshell].

##### 0. Install JDK 11 or later
Make sure you have JDK 11 or later installed and configured.
`jshell` should be executable from any directory and print its version via: 
```text
<path/> jshell --version
jshell 11.0.2
```

##### 1. Source `Bach.jsh` into JShell

Open a command shell and change into the directory containing your modular Java project. 

```text
<path/> jshell https://bit.ly/bach-jsh
```

:sparkles: That's all you need to build a modular Java project. :sparkles:

> Note: the shortened `https://bit.ly/bach-jsh` expands to https://raw.githubusercontent.com/sormuras/bach/master/src/bach/Bach.jsh

For immediate results, such as fail-fast on errors, use:

```text
jshell --execution=local https://bit.ly/bach-jsh
```

For more information what Bach.java is doing at runtime, use:

```text
jshell --execution=local -J-Debug https://bit.ly/bach-jsh
```

For more details consult the output of `jshell --help`.

## Directory Layout

:construction:

## bit.ly links

- [https://bit.ly/bach-java](https://bit.ly/bach-java) :wavy_dash: [src/bach/Bach.java](src/bach/Bach.java)
- [https://bit.ly/bach-jsh](https://bit.ly/bach-jsh) :wavy_dash: [src/bach/Bach.jsh](src/bach/Bach.jsh)
- [https://bit.ly/boot-bach](https://bit.ly/boot-bach) :wavy_dash: [boot.jsh](boot.jsh)

# install-jdk.sh

`install-jdk.sh` main purpose is to install the _latest-and-greatest_ available OpenJDK release from [jdk.java.net](https://jdk.java.net).

#### Options of `install-jdk.sh`
```
-h|--help                 Displays this help
-d|--dry-run              Activates dry-run mode
-s|--silent               Displays no output
-e|--emit-java-home       Print value of "JAVA_HOME" to stdout (ignores silent mode)
-v|--verbose              Displays verbose output

-f|--feature 9|11|...|ea  JDK feature release number, defaults to "ea"
-o|--os linux-x64|osx-x64 Operating system identifier
-u|--url "https://..."    Use custom JDK archive (provided as .tar.gz file)
-w|--workspace PATH       Working directory defaults to user's ${HOME}
-t|--target PATH          Target directory, defaults to first real component of the tarball
-c|--cacerts              Link system CA certificates (currently only Debian/Ubuntu is supported)
```

#### How to set `JAVA_HOME` with `install-jdk.sh`

- Source `install-jdk.sh` into current shell to install latest OpenJDK and let it update `JAVA_HOME` and `PATH` environment variables:

  - `source ./install-jdk.sh` _Caveat: if an error happens during script execution the calling shell will terminate_
  
- Provide target directory path to use as `JAVA_HOME`:

  - `JAVA_HOME=~/jdk && ./install-jdk.sh --target $JAVA_HOME && PATH=$JAVA_HOME/bin:$PATH`

- Run `install-jdk.sh` in a sub-shell to install latest OpenJDK and emit the installation path to `stdout`:

  - `JAVA_HOME=$(./install-jdk.sh --silent --emit-java-home)`
  - `JAVA_HOME=$(./install-jdk.sh --emit-java-home | tail --lines 1)`

#### JDK Build Matrix at Travis CI

See `sormuras.github.io`'s [Travic CI configuration](https://github.com/sormuras/sormuras.github.io/blob/master/.travis.yml) for details.

[![matrix](https://raw.githubusercontent.com/sormuras/sormuras.github.io/master/blog/2019-07-09-jdk-matrix-screenshot.png)](https://travis-ci.org/sormuras/sormuras.github.io)

# be free - have fun
[![jsb](https://upload.wikimedia.org/wikipedia/commons/thumb/6/65/Bachsiegel.svg/220px-Bachsiegel.svg.png)](https://wikipedia.org/wiki/Johann_Sebastian_Bach)

[jshell]: https://docs.oracle.com/en/java/javase/11/tools/jshell.html
[bach.jsh]: https://github.com/sormuras/bach/blob/master/bach.jsh
[boot.jsh]: https://github.com/sormuras/bach/blob/master/boot.jsh
[Bach.java]: https://github.com/sormuras/bach/blob/master/src/main/Bach.java
[install-jdk.sh]: https://github.com/sormuras/bach/blob/master/install-jdk.sh
