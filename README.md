# Bach.java 2.0-M7 - Java Shell Builder
 
[![jdk11](https://img.shields.io/badge/JDK-11+-blue.svg)](https://jdk.java.net)
[![experimental](https://img.shields.io/badge/API-experimental-yellow.svg)](https://jitpack.io/com/github/sormuras/bach/master-SNAPSHOT/javadoc/)
[![github actions](https://github.com/sormuras/bach/workflows/Bach.java/badge.svg)](https://github.com/sormuras/bach/actions)
[![jitpack](https://jitpack.io/v/sormuras/bach.svg)](https://jitpack.io/#sormuras/bach)
[![central](https://img.shields.io/maven-central/v/de.sormuras.bach/de.sormuras.bach.svg)](https://search.maven.org/artifact/de.sormuras.bach/de.sormuras.bach)
[![slides](https://img.shields.io/badge/Slide-deck-lightgray.svg)](https://gitpitch.com/sormuras/bach)

:scroll:Fast-forward to [install-jdk.sh](#install-jdksh) section.

Use Java source to build your modular Java project.

> No need to be a maven to be able to use a build tool - [forax/pro](https://github.com/forax/pro)

Ranging from [JDK Foundation Tools], over shell scripts and [Apache Ant] to multi-language, multi-purpose build tools...
![jdk-and-build-tools](doc/img/jdk-and-build-tools-with-bach.svg)

...`Bach.java`'s target is between platform-specific shell scripts and [Apache Ant].

**Simplistic** Use [Bach.java](https://github.com/sormuras/bach/blob/master/src/bach/Bach.java) as a Java-based script template and manually call [JDK Foundation Tools] -- just like in platform-specific shell scripts.
Call `load(URI)` to load 3rd-party modules, `run(String tool, String... args)` to run provided tools, and `start(String... command)` to execute commands on the command-line.

**Lightweight** Let module `de.sormuras.bach` automatically order the right calls to [JDK Foundation Tools] based on your `module-info.java` declarations.
- Installation-free via `jshell https://bit.ly/bach-jsh`.
- [Download](https://search.maven.org/artifact/de.sormuras.bach/de.sormuras.bach) module `de.sormuras.bach` to your `lib/` directory and use it directly.

## Uniques

- [x] [zero installation](#zero-installation) required (besides JDK 11+, using `jshell`)
- [x] [zero configuration](#zero-configuration) required (conventions and information gathered from `module-info.java` files)
- [x] [customize with properties](#customize-via-properties) to override auto-configured values (`.bach/project.properties`)
- [x] [b-y-o-b](#bring-your-own-build) program using plain old Java (`src/bach/Build.java`)
- [x] [3rd-party modules](#3rd-party-modules) in plain sight (single `lib/` directory)
- [x] [compilation](#compilation--compile--package) is compile (`javac`) and package (`jar`) as an atomic step
- [x] [multi-module](#multi-module) compilation in a single pass (`--module-source-path`, `${PROJECT.NAME}-javadoc.jar`)
- [x] [multi-release](#multi-release) modules are supported (`java-7`, `java-8`, ..., `java-11`, ..., `java-N`)
- [x] [automated checks](#automated-checks) (aka testing) built-in (`test(${MODULE})`,`junit`)
- [x] [deployment](#deployment) support (via `${MODULE}-sources.jar` and consumer `pom.xml` per module)

## Zero Installation

Enter the base directory of your Java project, open a shell, and execute one of the following commands:

- Long: `jshell https://raw.githubusercontent.com/sormuras/bach/master/src/bach/Bach.jsh`
- Shorter: `jshell https://github.com/sormuras/bach/raw/master/src/bach/Bach.jsh`
- Shortened: `jshell https://bit.ly/bach-jsh`

That's it.

## Zero Configuration

Almost all required information to build a modular Java project is either deduced from conventions or gathered from
module declarations, i.e. `module-info.java` files.

- Required modules' versions via https://github.com/sormuras/modules

Also, the following attributes are extracted from comments (soon annotations?) found in module declarations: 

- Module version `--module-version ...`
- Module entry-point `--main-class ...`

## Customize Via Properties

The default [ProjectBuilder](https://github.com/sormuras/bach/blob/master/src/de.sormuras.bach/main/java/de/sormuras/bach/project/ProjectBuilder.java)
implementation loads some properties from a `.bach/project.properties` file.
Here, you may set different values for project properties like `name`, `version`, etc.

## Bring Your Own Build

Does your project use a different structure?
Want to execute tasks is a different order?
Need additional tasks to be executed?
Want skip tests?

Write your own build program using plain old Java!

Store your build program as [`src/bach/Build.java`](https://github.com/sormuras/bach/blob/master/src/bach/Build.java) and [Bach.java](https://github.com/sormuras/bach/blob/master/src/bach/Bach.java#L43) will delegate to it.
To make your own build program runnable from within an IDE, you need to download and mount module `de.sormuras.bach` first.
After that simply run the `de.sormuras.bach` module.

## 3rd-party Modules

All 3rd-party modules are stored in plain sight: `lib/`
3rd-party modules are all modules that are not declared in your project and that are not modules provided by the _system_, i.e. the current Java runtime.

How do you provide 3rd-party modules?

- Load and drop modular JAR files into the `lib/` directory.

Missing 3rd-party modules are being resolved in a best-effort manner using [sormuras/modules](https://github.com/sormuras/modules) database.

## Compilation = Compile + Package

- `javac` + `jar`

Exploded class files are only a intermediate state.
This ensures, that at test runtime, you're checking your modules as if they are already published.
Including loading services and resources.
And [multi-release](#multi-release) modules.

## Multi Module

Using the `--module-source-path` option from `javac` all modules are compiled in a single pass.
With the exception [multi-release](#multi-release) modules - they are build before any other module.

## Multi Release

Organize your Java sources in targeted directories to create a multi-release JAR.

```text
src/${REALM}/
  java-7/
  java-8/
  java-9/
    module-info.java
  ...
  java-N/
```

Multi-release modules are compiled ahead of any other (read: normal Jigsaw-style) module.

## Automated Checks

Two kinds of automated checks per module are supported:

- Run a provided tool named `test(${MODULE})` via [ToolProvider](https://docs.oracle.com/javase/9/docs/api/java/util/spi/ToolProvider.html) SPI

  ```java
  module m {
    provides java.util.spi.ToolProvider with m.MyModuleTestProgram;
  }
  ```
  ```java
  package m; // in module m

  public class MyModuleTestProgram implements java.util.spi.ToolProvider {
    @Override
    public String name() {
      return "test(m)";
    }

    @Override
    public int run(java.io.PrintWriter out, java.io.PrintWriter err, String... args) {
      try {
        // HERE BE DRAGONS! Erm, calls to your "test" code.
        return 0;
      } catch (Throwable throwable) {
        throwable.printStackTrace(err);
        return 1;
      }
    }
  }
  ```

- Run JUnit Platform with selecting the current module under test

  ```java
  open /*test*/ module t {
    requires org.junit.jupiter; // pulls in API, Params, and more...
  }
  ```
  Write your JUnit 5 (read: Jupiter) tests as usual.

Bach supports full modular in-module testing with real test modules.
Test modules are declared as normal Java modules.
There's neither a need for an extra DSL nor resorting to command-line options via `module-info.test`.

```java
open /*test*/ module m /*extends "main" module*/ {

  // "test"

  requires org.junit.jupiter;

  // "main" -- below statements are copied from the main module declaration

  requires java.logging;
  requires java.net.http;
  requires java.xml;

  exports m.api;

  uses java.util.spi.ToolProvider;
}
```

Bach takes care to load and use the appropriate JUnit Platform modules and also resolves "missing" test engines.

## Deployment

- For each module a `${MODULE}-${VERSION}-sources.jar` is created.
- If a consumer `pom.xml` is stored in `src/${MODULE}/main/maven/`, Maven-based install and deploy scripts will be created.


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
[bach.jsh]: https://github.com/sormuras/bach/blob/master/bach.jsh
[install-jdk.sh]: https://github.com/sormuras/bach/blob/master/install-jdk.sh
[JDK Foundation Tools]: https://docs.oracle.com/en/java/javase/11/tools/main-tools-create-and-build-applications.html
[jshell]: https://docs.oracle.com/en/java/javase/11/tools/jshell.html
