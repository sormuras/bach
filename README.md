# Bach - Java Shell Builder - Builds (on(ly)) Modules

> "The tools we use have a profound (and devious!) influence on our thinking habits, and, therefore, on our thinking abilities."
>
> [Edsger W. Dijkstra, 18 June 1975](https://www.cs.virginia.edu/~evans/cs655/readings/ewd498.html)

Bach is a lightweight Java build tool that orchestrates [JDK tools] for building modular Java projects.

By default, it tries its best to call the right tool at the right time with the right arguments. Bach encourages
developers to explore, learn, and master these foundation tools with their options; in order to allow a good
understanding of what is really going on when building a Java project. Pass those learnings and optimizations as
fine-grained tweaks in a declarative manner back to Bach using pure Java syntax.

Fast-forward to sections: ♥ [Motivation](#motivation), ✔ [Goals](#goals), and ❌ [Non-Goals](#non-goals)

## Build Bach with Bach

- Install [JDK] 16 or later.

- Build
  ```text
  > java .bach/src/bootstrap.java
  > .bach/bin/bach build
  ```

## Motivation

The JDK contains a set of foundation tools but none of them guides developers from processing Java source files into
shippable products: be it a reusable modular JAR file with its API documentation or an entire custom runtime image.
There exists however an implicit workflow encoded in the available tools and their options. The (binary)
output of one tool is the input of one or more other tools. With the introduction of modules in Java 9 some structural
parts of that workflow got promoted into the language itself and resulted in explicit module-related tool options.

These structural information, encoded explicitly by developers in Java's module descriptors (`module-info.java` files),
serves as basic building blocks for Bach's project model. Their location within the file tree, their module name, and
their `requires` directives are examples of such information. In addition, Bach defines an API in order to let
developers define extra configuration information. Included are project-specific values such as a short name, a version
that is applied to all modules of the project, a path matcher defining where to find modules, and a lot more. Most of
these project-specific values have pre-defined default values; some of these values provide an auto-configuration
feature.

## Goals

Bach...

- builds Java projects.
- builds modular Java projects.
- is a lightweight wrapper for existing and future tools, mainly foundation tools provided by the JDK.
- supports running modularized tools registered via the `ToolProvider` SPI.
- supports running tools packaged in executable JAR files (via Java's `Process` API).
- can be invoked directly from the command line, or programmatically via its modular API (in a JShell session).
- infers basic project information from `module-info.java` files.
- uses standard Java syntax for extra configuration purposes.
- supports creation of multi-release JARs (via javac's and jar's `--release` option).
- helps resolve missing external dependences by downloading required modules into a single project-local directory.
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

# be free - have fun

[![jdk16](https://img.shields.io/badge/JDK-16-blue.svg)](https://jdk.java.net)
[![experimental](https://img.shields.io/badge/API-experimental-yellow.svg)](https://sormuras.github.io/api/bach/17-ea)
![GitHub all releases](https://img.shields.io/github/downloads/sormuras/bach/total)

[![jsb](https://upload.wikimedia.org/wikipedia/commons/thumb/6/65/Bachsiegel.svg/220px-Bachsiegel.svg.png)](https://wikipedia.org/wiki/Johann_Sebastian_Bach)

[JDK]: https://jdk.java.net

[JDK tools]: https://docs.oracle.com/en/java/javase/16/docs/specs/man/index.html

[JUnit]: https://junit.org
