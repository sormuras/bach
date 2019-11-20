# Bach.java - Java Shell Builder
 
[![jdk11](https://img.shields.io/badge/JDK-11+-blue.svg)](https://jdk.java.net)
[![experimental](https://img.shields.io/badge/API-experimental-yellow.svg)](https://jitpack.io/com/github/sormuras/bach/master-SNAPSHOT/javadoc/)
[![github actions](https://github.com/sormuras/bach/workflows/Bach.java/badge.svg)](https://github.com/sormuras/bach/actions)
[![jitpack](https://jitpack.io/v/sormuras/bach.svg)](https://jitpack.io/#sormuras/bach)

Use Java source to build your modular Java project.

```text
    ___      ___      ___      ___
   /\  \    /\  \    /\  \    /\__\
  /  \  \  /  \  \  /  \  \  / /__/_
 /  \ \__\/  \ \__\/ /\ \__\/  \/\__\
 \ \  /  /\/\  /  /\ \ \/__/\/\  /  /
  \  /  /   / /  /  \ \__\    / /  /
   \/__/    \/__/    \/__/    \/__/.java
```

> No need to be a maven to be able to use a build tool - [forax/pro](https://github.com/forax/pro)

:scroll:Fast-forward to [install-jdk.sh](#install-jdksh) section.

## U N D E R C O N S T R U C T I O N

_Reboot into version `2-ea` in progress..._

# install-jdk.sh

`install-jdk.sh` main purpose is to install the _latest-and-greatest_ available OpenJDK release from [jdk.java.net](https://jdk.java.net).
Find a Travis CI matrix configuration at [sormuras.github.io/.travis.yml](https://github.com/sormuras/sormuras.github.io/blob/master/.travis.yml). 

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

# be free - have fun
[![jsb](https://upload.wikimedia.org/wikipedia/commons/thumb/6/65/Bachsiegel.svg/220px-Bachsiegel.svg.png)](https://wikipedia.org/wiki/Johann_Sebastian_Bach)

[jshell]: https://docs.oracle.com/en/java/javase/11/tools/jshell.html
[bach.jsh]: https://github.com/sormuras/bach/blob/master/bach.jsh
[boot.jsh]: https://github.com/sormuras/bach/blob/master/boot.jsh
[Bach.java]: https://github.com/sormuras/bach/blob/master/src/main/Bach.java
[install-jdk.sh]: https://github.com/sormuras/bach/blob/master/install-jdk.sh
