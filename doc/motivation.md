## Motivation

From Project Jigsaw's [Module System Quick-Start Guide](https://openjdk.java.net/projects/jigsaw/quick-start) to [Bach.java](https://github.com/sormuras/bach) - a Java Shell Build tool.

The goal of each following build process examples is to compile, package, and link modular Java source files into a custom runtime image.

### Quick-Start Guide "Greetings"

Let's examine the single module project setup.

> **Greetings**
>
> This first example is a module named `com.greetings` that simply prints "Greetings!".
> The module consists of two source files: the module declaration (`module-info.java`) and the main class.

#### Hand-crafted Build Program

Let's replay the tool calls shown in the Quick-Start Guide Greetings section with minor simplifications.

First, the `src` directory is dropped.
Second, we enhance the message with the string representation of module `com.greetings`.

```text
doc/project/JigsawQuickStart
.
└───com.greetings
    │   module-info.java          --> | module com.greetings {}
    └───com
        └───greetings
                Main.java         --> | System.out.format("Greetings from %s!%n", Main.class.getModule());
```

Next, let's write down all calls to `javac`, `jar`, and `jlink` in a platform-agnostic manner making use of provided tools and Java's new source launch mode.
For that, Java 9 introduced the [ToolProvider](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/spi/ToolProvider.html) SPI.
Since Java 11 launching single-file source-code programs ([JEP 330](https://openjdk.java.net/jeps/330)) is supported.
As the minimal target Java runtime is 11, we're good to go.

Find the tool calls from the program that builds the first example from the Quick-Start Guide below.
All local variable declarations and helper method definitions are omitted for brevity reasons.
View the full source code of the build program at: [BuildJigsawQuickStart.java](src/BuildJigsawQuickStart.java)

```java
class BuildJigsawQuickStart {
  public static void main(String... args) {
    // ...
    runTool("javac", "--module=" + module, "--module-source-path=" + base, "-d", classes);
    // ...
    runTool("jar", "--create", "--file=" + file, "-C", classes.resolve(module), ".");
    // ...
    runTool("jlink", addModules, modulePath, launcher, output);
  }

  // Declaration of runTools() and other helper methods.
}
```

After launching the build program from the root directory via `java doc/src/BuildJigsawQuickStart.java`,
the tree structure of the generated files in directory `doc/project/JigsawQuickStart/build` looks like the following printout.

```
doc/project/JigsawQuickStart
.
├───build
│   ├───classes                       | `javac`'s output directory
│   │   └───com.greetings
│   │       │   module-info.class
│   │       └───com
│   │           └───greetings
│   │                   Main.class
|   |
│   ├───image                         | `jlink`'s output directory
│   │   │   release                   | Describes this image: `MODULES="java.base com.greetings"`
│   │   ├───bin                       | Contains `greet[.bat]` launcher
|   |   :                             |
│   │   └───lib                       | Contains linked `modules` file
|   |
│   └───modules                       | `jar`'s output directory
│           com.greetings.jar         | Modular JAR file
|
└───com.greetings
    │   module-info.java
    └───com
        └───greetings
                Main.java
```

Verify that the custom runtime image contains everything to greet us.

```shell script
build/image/bin/greet
Greetings from module com.greetings!
```

#### Enter **Bach.java**

TL;DR: Build

```shell script
jshell https://sormuras.de/bach/build
```

TL;DR: Launch

```shell script
.bach/workspace/image/bin/greetings
Greetings from module com.greetings!
```

**Bach.java** creates a build program on-the-fly, similar to the hand-crafted one shown above.
It plans JDK foundation tool calls in the right order and with the right arguments.
Opinionated right.
Learn how to control the call planning and bring in your own opinions will be documented in the [Bach.java API](https://javadoc.io/doc/de.sormuras.bach/de.sormuras.bach) documentation.
*Soon*™.

Listing the directory tree of `.bach` in `doc/project/JigsawQuickStart` yields:

 ```text
doc/project/JigsawQuickStart/.bach
.
├───src                               | Contains the custom build program
│       Build.java                    | Bach.of(project -> project).build();
│
└───workspace                         | Root of generated assets
    │   logbook.md                    | Last build summary, list of executed tasks, tool output, and more.
    │
    ├───classes
    │   └───com.greetings
    │       │   module-info.class
    │       :
    │
    ├───image
    │   ├───bin                       | Contains the greetings[.bat] launcher script
    |   :
    │
    ├───modules
    │       com.greetings.jar
    │
    :
```

### Quick-Start Guide "Greetings World"

Three distinct module names.
Two modules in main code space, two modules in test code space.

```text
├───com.greetings
│   └───main
│       │   module-info.java      --> | module com.greetings { ← ────────────────────────────────┐
│       └───com                       |   requires org.astro;  → ──┐                             │
│           └───greetings             | }                          |                             │
│                   Main.java                      ┌───────────────┘                             │
│                                                  │                                             │
├───org.astro                                      │ ┌───────────────────────────────────────────┤
│   ├───main                                       ↓ ↓                                           │
│   │   │   module-info.java      --> | module org.astro {   ├───┐ copy module name              │
│   │   └───org                       |   exports org.astro; ├───┼────────┐                      │
│   │       └───astro                 | }                        │        │ copy relevant        │
│   │               World.java                                   │        │ directives from      │
│   └───test                                                ┌────┴────┐   │ main module          │
│       │   module-info.java      --> | open /*test*/ module org.astro {  │                      │
│       └───org                       |   exports org.astro; ├────────────┘                      │
│           └───astro                 |   provides ...ToolProvider with org.astro.TestProvider;  │
│                   TestProvider.java | }                                                        │
│                                                                                                │
└───test.modules                                                                                 │
    └───test                                                                                     │
        │   module-info.java      --> | open /*test*/ module test.modules {                      │
        └───test                      |   requires com.greetings; → ─────────────────────────────┤
            └───modules               |   requires org.astro; → ─────────────────────────────────┘
                    TestProvider.java |   provides ...ToolProvider with test.modules.TestProvider;
                                      | }
```

Call `javac` + `jar`, `javadoc`, and `jlink`.

- [Build Jigsaw Quick-Start World](src/BuildJigsawQuickStartWorld.java)

```text
[main]
javac    --module=com.greetings,org.astro ...
jar      --file main/com.greetings.jar ...
jar      --file main/org.astro.jar
javadoc  --module=com.greetings,org.astro ...
jlink    --add-modules=com.greetings,org.astro ...

[test]
javac    --module=test.modules,org.astro ...
jar      --file test/test.modules.jar ...
jar      --file test/org.astro.jar ...
test(test.modules)
test(org.astro)
```

```
├───build
│   ├───api
│   │       index.html
|   |
│   ├───classes
│   │   ├───main
│   │   │   ├───com.greetings
│   │   │   └───org.astro
│   │   └───test
│   │       ├───org.astro
│   │       └───test.modules
|   |
│   ├───image
|   |
│   └───modules
│       ├───main
│       │       com.greetings.jar
│       │       org.astro.jar
│       └───test
│               org.astro.jar
│               test.modules.jar
|
├───com.greetings
├───org.astro
└───test.modules
```
