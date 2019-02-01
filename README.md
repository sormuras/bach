# bach - Java Shell Builder
 
[![jdk11](https://img.shields.io/badge/jdk-11-blue.svg)](http://jdk.java.net/11/)
[![travis](https://travis-ci.org/sormuras/bach.svg?branch=master)](https://travis-ci.org/sormuras/bach)
[![experimental](https://img.shields.io/badge/api-experimental-yellow.svg)](https://jitpack.io/com/github/sormuras/bach/master-SNAPSHOT/javadoc/)

Use Java source in [jshell] to build your modular project.

> No need to be a maven to be able to use a build tool - [forax/pro](https://github.com/forax/pro)

Fast-forward to [install-jdk.sh](#install-jdksh) section.

## bootstrap

```text
jshell https://bit.ly/boot-bach
bach help
```

# install-jdk.sh

`install-jdk.sh` main purpose is to install the _latest-and-greatest_ available OpenJDK release from [jdk.java.net](http://jdk.java.net).
It supports GA releases and builds provided by [Oracle](http://www.oracle.com/technetwork/java/javase/terms/license/index.html) as well. 

#### Options of `install-jdk.sh`
```
-h|--help                 Displays this help
-d|--dry-run              Activates dry-run mode
-s|--silent               Displays no output
-e|--emit-java-home       Print value of "JAVA_HOME" to stdout (ignores silent mode)
-v|--verbose              Displays verbose output

-f|--feature 9|10|...|ea  JDK feature release number, defaults to "ea"
-l|--license GPL|BCL      License defaults to "GPL"
-o|--os linux-x64|osx-x64 Operating system identifier (works best with GPL license)
-u|--url "https://..."    Use custom JDK archive (provided as .tar.gz file)
-w|--workspace PATH       Working directory defaults to user's ${HOME}
-t|--target PATH          Target directory, defaults to first component of the tarball
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



## be free - have fun
[![jsb](https://upload.wikimedia.org/wikipedia/commons/thumb/6/65/Bachsiegel.svg/220px-Bachsiegel.svg.png)](https://wikipedia.org/wiki/Johann_Sebastian_Bach)

[jshell]: https://docs.oracle.com/en/java/javase/11/tools/jshell.html
[boot.jsh]: https://github.com/sormuras/bach/blob/master/boot.jsh
[Bach.java]: https://github.com/sormuras/bach/blob/master/src/bach/Bach.java
[Bach.jsh]: https://github.com/sormuras/bach/blob/master/src/bach/Bach.jsh
[install-jdk.sh]: https://github.com/sormuras/bach/blob/master/install-jdk.sh
