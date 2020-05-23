# Bach.java 11-ea - Java Shell Builder
 
[![jdk11](https://img.shields.io/badge/JDK-11+-blue.svg)](https://jdk.java.net)
[![experimental](https://img.shields.io/badge/API-experimental-yellow.svg)](https://jitpack.io/com/github/sormuras/bach/master-SNAPSHOT/javadoc/)
[![github actions](https://github.com/sormuras/bach/workflows/Bach.java/badge.svg)](https://github.com/sormuras/bach/actions)

Use Java source to build your modular Java project.

## Features

- ☕ **Java**, pristine Java

    > JDK 11+ required.
    Write custom build programs in Java (no XML, YML, Z...).
    Java modules declarations define dependencies.
    Missing 3<sup>rd</sup>-party modules are resolved recursively.

- 🚀 **Zero-installation** build mode

    > `jshell https://sormuras.de/bach-build` - a copy of [bach-build.jsh](src/bach/bach-build.jsh)

- 📚 **API** documentation

    > Calls `javadoc` with the right arguments.
    Find generated HTML pages in `bach/workspace/api/`.

- 💾 Custom runtime **image**

    > Calls `jlink` with the right arguments.
    Find binary assets in `bach/workspace/image/`.

- ✔ Automated Checks: **Test** program and JUnit Platform support

    > Runs custom `ToolProvider`-based test programs named `test(${MODULE})`.
    In-process.
    Launches [JUnit Platform](https://junit.org/junit5/docs/current/user-guide/#running-tests-console-launcher) with the right arguments.
    In-process.
    Find reports in `.bach/workspace/junit-reports/`.

- 📋 Structured build **summary** with history

    > Stores latest summary as `.bach/workspace/summary.md`.
    Find a collection summary files from previous builds in `.bach/workspace/summaries/`.

### TODO

- ⌨ More main modes: `clean`, `help`, `info`...
- 🚧 Scaffold sample projects via shell script `bach-boot.jsh`
- 🗄 Multi-Release modular JAR file support
- 🧩 Locator looking up module-to-maven mappings from [sormuras/modules](https://github.com/sormuras/modules)

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
[jlink]: https://docs.oracle.com/en/java/javase/11/tools/jlink.html
[jshell]: https://docs.oracle.com/en/java/javase/11/tools/jshell.html
