# Bach - Java Shell Builder - Builds (on(ly)) Modules

> "The tools we use have a profound (and devious!) influence on our thinking habits, and, therefore, on our thinking abilities."
>
> [Edsger W. Dijkstra, 18 June 1975](https://www.cs.virginia.edu/~evans/cs655/readings/ewd498.html)

Bach is a lightweight Java build tool that orchestrates JDK foundation tools ([1]) for building modular Java projects.

By default, it tries its best to call the right tool at the right time with the right arguments. Bach encourages
developers to explore, learn, and master these tools and their options in order to allow a good understanding of what is
going on under the hood. Pass those learnings and optimizations as fine-grained tweaks in a declarative manner back to
Bach using pure Java syntax.

## Prelude

- Install [JDK] 16 or later.
- Open a console window and verify `java --version` is at least Java 16.
  ```text
  > java --version
  
  openjdk 16 2021-03-16
  OpenJDK Runtime Environment (build 16+36-2231)
  OpenJDK 64-Bit Server VM (build 16+36-2231, mixed mode, sharing)
  ```
- Create a new directory and change into it.
  ```text
  > mkdir air && cd air
  ```
- Initialize Bach using JShell's load file feature.
  ```text
  air> jshell https://bit.ly/bach-init
  ```
- Create and save a `module-info.java` file in the current directory.
  ```text
  air> copy con module-info.java <ENTER>
  module air {}<ENTER>
  <CTRL+Z>
  ```
- Let Bach build this project
  ```text
  air> .bach\bin\bach build

  Bach 17-ea+55ecf77 (file:///.../.bach/bin/)
  Build air 0
  Build 1 main module: foo
    javac    --module foo --module-version 0 --module-source-p...
    jar      --create --file .bach\workspace\modules\foo@0.jar...
  Check main modules
    jdeps    --check foo --multi-release BASE --module-path .b...
  Generate API documentation
    javadoc  --module foo --module-source-path foo=. -encoding...
    jar      --create --file .bach\workspace\documentation\air...
  Assemble custom runtime image
    jlink    --add-modules foo --module-path .bach\workspace\m...
  Build took 2.612s
  Logbook written to file:///F:/air/.bach/workspace/logbook.md
  ```

## Motivation

The JDK contains a set of tools ([1]) but none of them guides developers from processing Java source files into
shippable products: be it a reusable modular JAR file with its API documentation or an entire custom (soon static [2])
runtime image. There exists however an implicit workflow encoded in the available tools and their options. The (binary)
output of one tool is the input of one or more other tools. With the introduction of modules in Java 9 some structural
parts of that workflow got promoted into the language itself and resulted in explicit module-related tool options.

These structural information, encoded explicitly by developers in Java's module descriptors (`module-info.java` files),
serves as basic building blocks for Bach's project model. Their location within the file tree, their module name, and
their `requires` directives are examples of such information. In addition, Bach defines an annotation
named `ProjectInfo` in order to let developers define extra configuration information. Included are project-specific
values such as a short name, a version that is applied to all modules of the project, a path matcher defining where to
find modules, and a lot more. Most of these project-specific values have pre-defined default values; some of these
values provide an auto-configuration feature.

Here's an excerpt of [Bach's project-info](.bach/bach.info/module-info.java) module declaration:

```java

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

# be free - have fun

[![jdk16](https://img.shields.io/badge/JDK-16-blue.svg)](https://jdk.java.net)
[![experimental](https://img.shields.io/badge/API-experimental-yellow.svg)](https://sormuras.github.io/api/bach/17-ea)

[![jsb](https://upload.wikimedia.org/wikipedia/commons/thumb/6/65/Bachsiegel.svg/220px-Bachsiegel.svg.png)](https://wikipedia.org/wiki/Johann_Sebastian_Bach)

[1]: https://docs.oracle.com/en/java/javase/16/docs/specs/man/index.html

[2]: https://mail.openjdk.java.net/pipermail/discuss/2020-April/005429.html

[3]: https://openjdk.java.net/projects/jigsaw/quick-start#greetingsworld

[JDK]: https://jdk.java.net