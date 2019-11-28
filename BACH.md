# Java Shell Builder

Ideas, thoughts, and more on the architecture of `Bach.java`.

## Uniques

- [x] [zero installation](#zero-installation) required (besides JDK 11+, using `jshell`)
- [x] [zero configuration](#zero-configuration) required (conventions and information gathered from `module-info.java` files)
- [x] [customize with properties](#customize-via-properties) to override auto-configured values (`.bach/project.properties`)
- [x] [b-y-o-b](#bring-your-own-build) program using plain old Java (`src/bach/Build.java`)
- [x] [3rd-party modules](#3rd-party-modules) in plain sight (single `lib/` directory)
- [x] [compilation](#compilation--compile--package) is compile (`javac`) and package (`jar`) as an atomic step
- [x] [multi-module](#multi-module) compilation in a single pass (`--module-source-path`, `${PROJECT.NAME}-javadoc.jar`)
- [x] [multi-release](#multi-release) module are supported (`java-7`, `java-8`, ..., `java-11`, ..., `java-N`)
- [x] [automated checks](#automated-checks) aka testing built-in (`test(${MODULE})`,`junit`)
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
        // HERE BE DRAGONS! Erm, "test" code.
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

Bach.java supports full modular in-module testing with real test modules.
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

## Deployment

- For each module a `${MODULE}-${VERSION}-sources.jar` is created.
- If a consumer `pom.xml` is stored in `src/${MODULE}/main/maven/`, Maven-based install and deploy scripts will be created.

## Singe-File Source-Code program or modular library?

- `Bach.java`
  - Single source of everything
  - Zero-installation, direct usage via `jshell https://...`
  - Stored in project under `.bach/src/Bach.java`, it is...
    - runnable via `java .bach/src/Bach.java action [args]`
    - mount- and runnable within an IDE    
  - Customizable by...
    - override `Bach.Project Bach.project()` to create a custom project model
    - implement `void Bach.custom(String...)` to declare a custom entry-point, by-passing `main()`

- `module de.sormuras.bach`
  - Defined API, available via [Maven Central](https://search.maven.org/artifact/de.sormuras.bach/de.sormuras.bach)
  - Installation required: needs bootstrap launcher, via `load(lib, ...)`, like `Bach[Wrapper].java`
  - Stored in project as `lib/de.sormuras.bach-${VERSION}.jar`, it is...
    - runnable via `java -p lib -m de.sormuras.bach action [args]`
    - mount- and runnable within an IDE
  - Customizable by...
    - implement `module build {requires de.sormuras.bach;}` and provide custom project instance
      and services
  - Types can be merged to a single-file source-code `Bach.java` program via `Merge.java` to
    support a similar setup
