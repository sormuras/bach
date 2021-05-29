# Bach - Java Shell Builder - Builds (on(ly)) Modules

> "The tools we use have a profound (and devious!) influence on our thinking habits, and, therefore, on our thinking abilities."
>
> [Edsger W. Dijkstra, 18 June 1975](https://www.cs.virginia.edu/~evans/cs655/readings/ewd498.html)

Bach is a lightweight Java build tool that orchestrates [JDK tools] for building modular Java projects.

By default, it tries its best to call the right tool at the right time with the right arguments. Bach encourages
developers to explore, learn, and master these foundation tools and their options in order to allow a good understanding
of what is really going on when building a Java project. Pass those learnings and optimizations as fine-grained tweaks
in a declarative manner back to Bach using pure Java syntax.

Fast-forward to sections: ♥ [Motivation](#motivation), ✔ [Goals](#goals), and ❌ [Non-Goals](#non-goals)

## Prelude & Build bach with bach

- Install [JDK] 16 or later. For convenient JDK switching install bhttps://sdkman.io and choose a 16-open.
  ```text
  > sdk install java 16-open

- Open a terminal and verify `java --version` and `jshell --version` reports at least Java 16.
  ```text
  > java --version                                          > jshell --version
  openjdk 16 2021-03-16                                     jshell 16
  ```
- Create a new directory and change into it.
  ```text
  > mkdir air && cd air
  ```
- Clone
  ```text
  > git clone https://github.com/sormuras/bach.git

  ```
- Build 
Change into folder & build 
  ```text
  > cd Bach
  > java bootstrap.java
  
  On Windows try                                            UNIX/Mac Machines try 
  > .bach\bin\bach build                                  .bach/bin/bach build

## Motivation

The JDK contains a set of foundation tools but none of them guides developers from processing Java source files into
shippable products: be it a reusable modular JAR file with its API documentation or an entire custom runtime image.
There exists however an implicit workflow encoded in the available tools and their options. The (binary)
output of one tool is the input of one or more other tools. With the introduction of modules in Java 9 some structural
parts of that workflow got promoted into the language itself and resulted in explicit module-related tool options.

These structural information, encoded explicitly by developers in Java's module descriptors (`module-info.java` files),
serves as basic building blocks for Bach's project model. Their location within the file tree, their module name, and
their `requires` directives are examples of such information. In addition, Bach defines an annotation
named `ProjectInfo` in order to let developers define extra configuration information. Included are project-specific
values such as a short name, a version that is applied to all modules of the project, a path matcher defining where to
find modules, and a lot more. Most of these project-specific values have pre-defined default values; some of these
values provide an auto-configuration feature.

Here's an excerpt of [.bach/bach.info/module-info.java](.bach/bach.info/module-info.java) module declaration
(using Bach's `17-ea-2` syntax):

```java
import com.github.sormuras.bach.ProjectInfo;
import com.github.sormuras.bach.ProjectInfo.*;

@ProjectInfo(
    name = "bach",     // short name of the project, defaults to current working directory's name
    version = "17-ea", // is often overridden via CLI's `--project-version 17-ea+1c4b8cc` option

    modules = "*/main/java", // a glob or regex pattern describing paths to module-info.java files
    compileModulesForJavaRelease = 16,    // support releases are 8..17 (consult `javac --help`)
    includeSourceFilesIntoModules = true, // treat source folders as resource folders
    tools = @Tools(skip = "jlink"),       // limit and filter executable tools by their names

    tweaks = {         // a set of tool-specific tweaks amending the computed tool call arguments
        @Tweak(tool = "javac", option = "-encoding", value = "UTF-8"), // JEP 400 will kill this line
        @Tweak(tool = "javac", option = "-g"),
    //...
)
module bach.info {
  requires com.github.sormuras.bach;
}
```

_Yes, Bach builds Bach..._

```text
Bach 17+BOOTSTRAP+2021-03-15T08:22:58.121801746Z (file:///home/runner/work/bach/bach/.bach/bin/)
Build bach 17-ea+1c4b8cc
Verify external modules located in file:///home/runner/work/bach/bach/.bach/external-modules/
Verified 11 external modules
Build 1 main module: com.github.sormuras.bach
  javac    --release 16 --module com.github.sormuras.bach --module-version 17-ea+1c4b8cc --module-source..
  jar      --create --file .bach/workspace/modules/com.github.sormuras.bach@17-ea.jar --main-class com.g..
Check main modules
  jdeps    --check com.github.sormuras.bach --multi-release BASE --module-path .bach/workspace/modules:...
Generate API documentation
  javadoc  --module com.github.sormuras.bach --module-source-path ./*/main/java --module-path .bach/exte..
  jar      --create --file .bach/workspace/documentation/bach-api-17-ea+1c4b8cc.zip --no-manifest -C .ba..
Generate and check Maven consumer POM file
  pomchecker check-maven-central --file /home/runner/work/bach/bach/.bach/workspace/deploy/maven/com.git..
Build 4 test modules: com.github.sormuras.bach, test.base, test.integration, test.projects
  javac    --module com.github.sormuras.bach,test.base,test.integration,test.projects --module-source-pa..
  jar      --verbose --create --file .bach/workspace/modules-test/test.projects@17-ea+test.jar -C .bach/..
  jar      --verbose --create --file .bach/workspace/modules-test/test.base@17-ea+test.jar -C .bach/work..
  jar      --verbose --create --file .bach/workspace/modules-test/test.integration@17-ea+test.jar -C .ba..
  jar      --verbose --create --file .bach/workspace/modules-test/com.github.sormuras.bach@17-ea+test.ja..
Launch JUnit Platform for each module
  junit    --select-module com.github.sormuras.bach --fail-if-no-tests --reports-dir .bach/workspace/rep..
  junit    --select-module test.base --fail-if-no-tests --reports-dir .bach/workspace/reports/junit/test..
  junit    --select-module test.integration --fail-if-no-tests --reports-dir .bach/workspace/reports/jun..
  junit    --select-module test.projects --fail-if-no-tests --config junit.jupiter.execution.parallel.en..
Build took 18.788s
Logbook written to file:///home/runner/work/bach/bach/.bach/workspace/logbook.md
```

## Goals

Bach...

- builds Java projects.
- builds modular Java projects.
- is a lightweight wrapper for existing and future tools, mainly foundation tools provided by the JDK.
- supports running modularized tools registered via the `ToolProvider` SPI.
- supports running tools packaged in executable JAR files (via Java's `Process` API).
- can be invoked directly from the command line, or programmatically via its modular API (in a JShell session).
- infers basic project information from `module-info.java` files.
- uses standard Java syntax for extra configuration purposes (via `@ProjectInfo`).
- supports creation of multi-release JARs (via javac's and jar's `--release` option).
- helps resolving missing external dependences by downloading required modules into a single project-local directory.
- launches the [JUnit] Platform Console (if provided as `junit` tool by the project).

## Non-Goals

Bach will **not** support "all features known from other build tools".

> If a feature F is not provided by an underlying tool, Bach will not support F.

Bach will **not**...

- support non-Java projects.
- support non-modular Java projects.
- provide a GUI for the tool.
- resolve conflicting external dependencies.
- deploy modules to external hosting services.

## Configuration And Customization

- Declare `module-info.java` files.
- Use Bach's CLI arguments to configure a specific build run. Consult the message produced by `bach --help` for
  available flags and options.
- Use `@ProjectInfo` on module `bach.info` (locacted at `.bach/bach.info/module-info.java`) to declare static
  configuration.
- Extend `Bach` to augment and alter the default behaviour or even write your own build program.

# be free - have fun

[![jdk16](https://img.shields.io/badge/JDK-16-blue.svg)](https://jdk.java.net)
[![experimental](https://img.shields.io/badge/API-experimental-yellow.svg)](https://sormuras.github.io/api/bach/17-ea)

[![jsb](https://upload.wikimedia.org/wikipedia/commons/thumb/6/65/Bachsiegel.svg/220px-Bachsiegel.svg.png)](https://wikipedia.org/wiki/Johann_Sebastian_Bach)

[JDK]: https://jdk.java.net

[JDK tools]: https://docs.oracle.com/en/java/javase/16/docs/specs/man/index.html

[JUnit]: https://junit.org
