# Bach.java 11.0-ea - Java Shell Builder
 
[![jdk11](https://img.shields.io/badge/JDK-11+-blue.svg)](https://jdk.java.net)
[![experimental](https://img.shields.io/badge/API-experimental-yellow.svg)](https://jitpack.io/com/github/sormuras/bach/master-SNAPSHOT/javadoc/)
[![github actions](https://github.com/sormuras/bach/workflows/Bach.java/badge.svg)](https://github.com/sormuras/bach/actions)

:scroll:Fast-forward to [install-jdk.sh](#install-jdksh) section.

Use Java source to build your modular Java project.

> No need to be a maven to be able to use a build tool - [forax/pro](https://github.com/forax/pro)

Ranging from [JDK Foundation Tools], over shell scripts and [Apache Ant] to multi-language, multi-purpose build tools...
![jdk-and-build-tools](doc/img/jdk-and-build-tools-with-bach.svg)

...`Bach.java`'s target is between platform-specific shell scripts and [Apache Ant].

# R E B O O T I N G . . .

Experimental is experimental is experimental.

## Build with Bach.java 11.0-ea on-the-fly 

Use `https://bit.ly/bach-build` as `<load-file>` argument for `jshell`:

- `jshell https://bit.ly/bach-build`

The shortened URL expands to:

- `jshell https://github.com/sormuras/bach/raw/master/src/bach/build.jsh`

### Build with Bach.java 2.1 on-the-fly

Bach.java 2.x was showcased in a lightning talk at [FOSDEM2020](https://fosdem.org/2020/schedule/event/bach).
To build your project with this effectively deprecated version of Bach.java, invoke:

- `jshell https://github.com/sormuras/bach/raw/2.1/src/bach/build.jsh`

## Common Conventions

- **Main Class** Convention\
A compilation unit named `Main.java` and located in a package with the same name as its module is considered to be the main class of that module.
For example: a module `com.greetings` (with a `module-info.java` in `src/com.greetings`) provides the main class `com.greetings.Main` if there's a `src/com.greetings/com/greetings/Main.java` compilation unit.

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

[Apache Ant]: https://ant.apache.org
[install-jdk.sh]: https://github.com/sormuras/bach/blob/master/install-jdk.sh
[JDK Foundation Tools]: https://docs.oracle.com/en/java/javase/11/tools/main-tools-create-and-build-applications.html
[jshell]: https://docs.oracle.com/en/java/javase/11/tools/jshell.html
