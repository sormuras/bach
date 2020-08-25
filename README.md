# Bach.java 11-ea - Java Shell Builder
 
[![jdk11](https://img.shields.io/badge/JDK-11+-blue.svg)](https://jdk.java.net)
[![experimental](https://img.shields.io/badge/API-experimental-yellow.svg)](https://jitpack.io/com/github/sormuras/bach/master-SNAPSHOT/javadoc/)
[![github actions](https://github.com/sormuras/bach/workflows/Bach.java/badge.svg)](https://github.com/sormuras/bach/actions)
[![central](https://img.shields.io/maven-central/v/de.sormuras.bach/de.sormuras.bach.svg)](https://search.maven.org/artifact/de.sormuras.bach/de.sormuras.bach)


Build modular Java projects with [JDK Foundation Tools].

## Features

- ☕ **Java**, pristine Java

    > Describe your project in Java's syntax.
    No XML, no YML, no Z... nor any another programming language required.

- 🧩 **Modules**, modules, everywhere!

    > Java modules as basic building blocks.
    Write `module-info.java` files to define main, test, and test-preview modules.

- 🚀 **Zero-installation** build mode

    > Invoke `jshell https://sormuras.de/bach/build` from the command line.
    To find out what other scripts are available, refer to [JShell Scripts](#jshell-scripts).

## JShell Scripts

Bach provides a set of JShell-based script files in the [src/bach](src/bach) folder.

All script files start with the `bach-` prefix, continue with the actual `${NAME}` of script and end with the `.jsh` file extension.
To execute one of these scripts use it as a `load-file` argument for `jshell`.

In order to save some type work, `https://sormuras.de/bach` provides a convenient redirection service.
For example, to execute the `build` script from the command line, you would call:

> `jshell https://sormuras.de/bach/build`

The redirect expands the URI to the long form: 

> `https://github.com/sormuras/bach/raw/HEAD/src/bach/bach-build.jsh`

You may refer to a specific version by adding a `@${VERSION}` tag to the short URI:

> `jshell https://sormuras.de/bach@11.7/build`

The targeted redirect expands the URI to the long form with the version tag replacing the `HEAD` path element: 

> `https://github.com/sormuras/bach/raw/11.7/src/bach/bach-build.jsh`

### JShell Action Scripts

- `bach-boot.jsh` - Launch a JShell session with module `de.sormuras.bach` loaded and ready to use.
- `bach-build.jsh` - Build a modular Java project with out installing Bach.
- `bach-help.jsh` - Print Bach's CLI help screen.
- `bach-init.jsh` - Install module `de.sormuras.bach` in the current working directory.
- `bach-pull.jsh` - Replace module `de.sormuras.bach` in the `.bach/lib` directory.

Find more details at [JShell Action Scripts](doc/jshell-action.md).

### JShell Demo Project Generator Scripts

- `bach-demo-0.jsh` - Simplicissimus, simply a single `module-info.java` file.
- `bach-demo-1.jsh` - Greetings! Inspired by Project Jigsaw: Module System Quick-Start Guide
- `bach-demo-2.jsh` - Greetings World! Inspired by Project Jigsaw: Module System Quick-Start Guide
- `bach-demo-5.jsh` - Multi-module project with modular tests driven by JUnit 5.
- `bach-demo-99.jsh` - 99 Luftballons, or a hundred module descriptors, each reading all previously defined

## Build Bach.java with Bach.java

- Install JDK 14 or newer
- Call `java .bach/src/build/build/Bootstrap.java`

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

[JDK Foundation Tools]: https://docs.oracle.com/en/java/javase/14/docs/specs/man
