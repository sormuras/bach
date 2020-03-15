# Bach.java 11.0-ea - Java Shell Builder
 
[![jdk11](https://img.shields.io/badge/JDK-11+-blue.svg)](https://jdk.java.net)
[![experimental](https://img.shields.io/badge/API-experimental-yellow.svg)](https://jitpack.io/com/github/sormuras/bach/master-SNAPSHOT/javadoc/)
[![github actions](https://github.com/sormuras/bach/workflows/Bach.java/badge.svg)](https://github.com/sormuras/bach/actions)

Use Java source to build your modular Java project.

Everything's written in standard `.java` files.
No additional `.xml`, `.yml`, or `.z...` configuration files required.
`Bach.java` infers many projects properties directly from the `module-info.java` compilation units.
On top of that, `Bach.java` supports an installation-free on-the-fly run mode via:

```shell script
jshell https://bit.ly/bach-build
```

Declare your own custom build program in `src/.bach/Build.java` and override various project properties using a builder API.
Your IDE of choice offers available property setters by default.
```java
class Build {
  public static void main(String... args) {
    new Bach().build(project -> project.version("1-ea")).assertSuccessful();
  }
}
```

`Bach.java` writes a build summary file in markdown format stores it in `.bach/summary.md`.
It contains amongst other information the structure of the project and the system properties at invocation-time.
The calls to [JDK Foundation Tools] with their arguments are also recorded:

|    |Thread|Duration|Caption
|----|-----:|-------:|-------
|   +|     1|        | Build project demo 1-ea
|    |     1|       0| **Create directories .bach**
|   +|     1|        | Print version of various foundation tools
|    |    16|      21| **Run `javac` with 1 argument(s)**
|    |    15|      23| **Run `jar` with 1 argument(s)**
|    |     1|      52| **Run `javadoc` with 1 argument(s)**
|   =|     1|      56| Print version of various foundation tools
|    |     1|      37| **Resolve missing modules**
|    |      |        | ...
|   +|     1|        | **Launch all tests**
|    |      |        | ...
|   =|     1|     105| Build project demo 1-ea done.
Legend
 - A row starting with `+` denotes the start of a task container.
 - A blank row start (` `) is a normal task execution. Its caption is emphasized.
 - A row starting with `X` marks an erroneous task execution.
 - A row starting with `=` marks the end (sum) of a task container.
 - The Thread column shows the thread identifier, with `1` denoting main thread.
 - Duration is measured in milliseconds.

## Example Projects

After cloning or downloading an example project, open a shell in the base directory of the project and call `jshell https://bit.ly/bach-build`.

That's all.
Have fun!

- [📋 bach-template](https://github.com/sormuras/bach-template) - Minimal Java project template

  A minimal modular Java project that contains a single and almost empty `module-info.java` file.

- [☁ bach-air](https://github.com/sormuras/bach-air) - Java project with inter-module and in-module tests

  A modular Java project that show-cases inter-module (black-box) and in-module (white-box) testing.
  It also provides IntelliJ IDEA configuration files with shared test run launchers for both scenarios.

- [⭐ bach-javafx](https://github.com/sormuras/bach-javafx) - Demo based on [HelloFX/CLI](https://github.com/openjfx/samples/tree/master/HelloFX/CLI) by OpenJFX

  A single-module Java application leveraging [OpenJFX](https://openjfx.io).
  A custom runtime image is created via [jlink] for the current platform.

All example projects usually contain a `.github/workflows/build.yml` configuration file that builds the project using `Bach.java` on push events.

## Build your project with Bach.java 11.0-ea on-the-fly 

Use `https://bit.ly/bach-build` as `<load-file>` argument for `jshell`:

- `jshell https://bit.ly/bach-build`

The shortened URL expands to:

- `jshell https://github.com/sormuras/bach/raw/master/src/bach/build.jsh`

Set system property `debug` to `""` (an empty string) or `true` to enable verbose output of the build run.
The prefix `-R` is required when running `Bach.java` on-the-fly via [jshell] as it launches a remote runtime system per default.

- `jshell -R-Debug https://bit.ly/bach-build`

### Build with Bach.java 2.1 on-the-fly

Bach.java 2.x was showcased in a lightning talk at [FOSDEM2020](https://fosdem.org/2020/schedule/event/bach).
To build your project with this effectively deprecated version of Bach.java, invoke:

- `jshell https://github.com/sormuras/bach/raw/2.1/src/bach/build.jsh`

## Common Conventions

- **Main Class** Convention\
A compilation unit named `Main.java` and located in a package with the same name as its module is considered to be the main class of that module.
For example: a module `com.greetings` (with a `module-info.java` in `src/com.greetings`) provides the main class `com.greetings.Main` if there's a `src/com.greetings/com/greetings/Main.java` compilation unit.

- **Main Module** Convention\
If the project declares exactly a single module with a main class, that module is considered to be the project main module.

- **Test Module `ToolProvider`** Convention\
If a test module provides a `java.util.spi.ToolProvider` implementation named like that (or another) test module, it is run.
For example: the `TestProvider` class is instantiated and run if module `test.modules` declares `provides java.util.spi.ToolProvider with test.modules.TestProvider;` and `TestProvider#getName()` returns `test(test.modules)`.

- **Mend Missing Modules** Convention\
If a project declares dependence to members of a set of well-known "API" modules, that for themselves require more modules to be present at runtime, those additional modules are implicitly declared as a dependence.
For example: a project module declares `requires org.junit.jupiter.api;` then `org.junit.jupiter.engine` is added to the set of required modules.

## Motivation

> No need to be a maven to be able to use a build tool - [forax/pro](https://github.com/forax/pro)

Ranging from [JDK Foundation Tools], over shell scripts and [Apache Ant] to multi-language, multi-purpose build tools...
![jdk-and-build-tools](doc/img/jdk-and-build-tools-with-bach.svg)

...`Bach.java`'s target is between platform-specific shell scripts and [Apache Ant].
`Bach.java` delegates

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
