# Call for Discussion: jbuild

Hi.

I would like to discuss the possible creation of a lightweight build tool for
modular Java projects included in the JDK itself: jbuild

The JDK contains a set of foundation tools [1] but none of them guides
developers from processing Java source files into shippable products: be it a
reusable modular JAR file with its API documentation or an entire custom (soon
static [2]) runtime image. There exists an implicit workflow encoded in the
available options of the foundation tools. The (binary) output of one tool is
the input of one or more tools. With the introduction of modules in Java 9 some
structural parts of that workflow got a) promoted into the language itself and
b) resulted in explicit module-related tool options. 

These structural information, encoded explicitly by developers in Java's module
descriptors, can be used as basic building blocks when describing a modular
Java project. I think of it as a "project-info.java" file -- which I don't
propose to introduce (as a part of the language) -- but it helps to transport
the jbuild idea. With assets from the "Greetings world" example [3] a
fictitious project descriptor could read like:

```
project greet {
  version 1-ea;
  modules com.greetings,org.astro;
  main-module com.greetings;

  module com.greetings {
    main-class com.greetings.Main;
  }
}
```

Based on such a project descriptor jbuild should call existing (and future) JDK
tools in the right order with the right arguments. Nothing more, nothing less.

It would expand to the following (trimmed) foundation tool calls by parsing
basic information about the modular project structure from module-info.java
and other source files:

```
javac --module com.greetings,org.astro
jar --file com.greetings@1-ea.jar --main-class com.greetings.Main
jar --file org.astro@1-ea.jar

javadoc --module com.greetings,org.astro
jar --file greet@1-ea-api.jar

jlink --add-modules com.greetings,org.astro --launcher greet=com.greetings
```

Find a more detailed motivation description at [4].

## Goals

jbuild targets modular Java projects that follow common practices and patterns
in structure and build steps; as defined and supported by the underlying
foundation tools.

The goal is to create a build tool that...

- is a lightweight wrapper for existing and future foundation tools of the JDK.
- can be invoked directly from the command line, or programmatically, either
  via the `ToolProvider` SPI or via its modular API (in a JShell session).
- infers basic project information from `module-info.java` files.
- uses standard Java syntax for configuration purposes
- supports creation of MR-JAR modules.
- helps resolving missing external dependencies by downloading required modules
  into a single project-local directory.
- knows how to run test modules via the `ToolProvider` API.
- launches the JUnit Platform [5] (if provided by the project).

## Non-Goals

There will be no support for "all features known from other build tools".

If a feature F is not already provided by a foundation tool, this build tool
will not support F. If F is required to build modular Java projects, F should
be implemented by a foundation tool If F is absolutely required to build
modular Java projects and its implementation would induce changes in multiple
foundation tools, this build tool will support F.

The build tool will/should/must **not**...

- support non-Java projects.
- support non-modular Java projects.
- provide a GUI for the tool.
- resolve conflicting external dependencies.
- deploy modules to external services.

Comments?

Cheers,
Christian

[1]: https://docs.oracle.com/en/java/javase/14/docs/specs/man/index.html
[2]: https://mail.openjdk.java.net/pipermail/discuss/2020-April/005429.html
[3]: https://openjdk.java.net/projects/jigsaw/quick-start#greetingsworld
[4]: https://github.com/sormuras/bach/blob/11.7/doc/motivation.md
[5]: https://junit.org/junit5
