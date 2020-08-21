# Call for Discussion: jbuild

Hi.

I would like to discuss the possible creation of a lightweight build tool for
modular Java projects included in the JDK itself.

The JDK contains a set of tools [1] but none of them guides developers from
processing Java source files into shippable products: be it a reusable modular
JAR file with its API documentation or an entire custom (soon static [2])
runtime image.

The proposed build tool, jbuild, should call existing (and future) tools in the
right order with the right arguments. With assets from the "Greetings world"
example [3] a jbuild call could read like:

`jbuild --project world --module com.greetings,org.astro`

It would expand to these (trimmed) foundation tool calls:

```
javac --module com.greetings,org.astro
jar --file com.greetings.jar
jar --file org.astro.jar

javadoc --module com.greetings,org.astro
jar --file world-api.jar

jlink --add-modules com.greetings,org.astro
```

Find a more detailed motivation description at [4].

## Goals

Create a build tool that...

- is a lightweight wrapper for existing and future foundation tools of the JDK.
- can be invoked directly from the command line, or programmatically, either
  via the `ToolProvider` SPI or via its modular API (in a JShell session).
- infers basic project information from `module-info.java` files.
- uses standard Java syntax for configuration purposes.
- supports creation of MR-JAR libraries.
- knows how to run test modules via the `ToolProvider` API.
- optionally launches JUnit Platform-based test engines.

## Non-Goals

There will be no support for "all features known from other build tools".

If a feature F is not already provided by a foundation tool, this build tool
will not support F. If F is required to build modular Java projects, F should
be implemented by a foundation tool If F is absolutely required to build
modular Java projects and its implementation would induce changes in multiple
foundation tools, this build tool will support F.

The proposed build tool will/should/must **not**...

- support non-modular Java projects.
- provide a GUI for the tool; existing IDEs will eventually
- invent new "standalone" features.
- resolve conflicting external dependencies.
- deploy modules.

Comments?

Cheers,
Christian

[1]: https://docs.oracle.com/en/java/javase/14/docs/specs/man/index.html
[2]: https://mail.openjdk.java.net/pipermail/discuss/2020-April/005429.html
[3]: https://openjdk.java.net/projects/jigsaw/quick-start#greetingsworld
[4]: https://github.com/sormuras/bach/blob/11.7/doc/motivation.md
