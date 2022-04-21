# JDK Tools and Where to Find Them

Java projects can be built using tools the Java Development Kit (JDK) provides.
Let's write a Java Scripting program that uses tools as primitives.

The JDK does not include a "build" tool.

Users resort to 3rd-party tools in order to transform Java source code into shippable products.

The JDK does include a set of foundation tools: javac, jar, ..., jlink.

This session explores ways how to make 
the implicit connection between JDK tools and Java modules explicit
so that users are enabled to build Java projects more easily.

## Outline

This session starts with an overview of 28+ tools shipping with JDK 18.

After a short break (reading all linked man pages takes time),
a few tools are executed live on the command-line in order to
compile and link an example application.

In order to execute command-line tools programmatically,
the `ToolProvider` interface was introduced in Java 9.
It provides a way invoke tools without necessarily starting a new VM. 

Leveraging the "Launch Single-File Source-Code Programs" feature
of JDK Enhancement Proposal (JEP) 330 a standalone tool-running
program called `Tool.java` is developed in seven steps.

Three abstractions are introduced along the way:

- `ToolFinder` A finder of tools. Similar to what `ModuleFinder` is to `ModuleReference`.
- `ToolRunner` A runner of tools. Uses `ToolFinder` and provides run time context.
- `ToolOperator` An extension of `ToolProvider` to run tools within a tool run.

## Tools, Options, Examples

[JDK 18 Documentation](https://docs.oracle.com/en/java/javase/18/)

[Java® Platform, Standard Edition & Java Development Kit Specifications Version 18](https://docs.oracle.com/en/java/javase/18/docs/specs)

[Java® Development Kit Version 18 Tool Specifications](https://docs.oracle.com/en/java/javase/18/docs/specs/man)

### Java Development Kit Version 18 Tool Specifications

**All Platforms**

* `jar` - create an archive for classes and resources, and manipulate or restore individual classes or resources from an archive
* `jarsigner` - sign and verify Java Archive (JAR) files
* `java` - launch a Java application
* `javac` - read Java class and interface definitions and compile them into bytecode and class files
* `javadoc` - generate HTML pages of API documentation from Java source files
* `javap` - disassemble one or more class files
* `jcmd` - send diagnostic command requests to a running Java Virtual Machine (JVM)
* `jconsole` - start a graphical console to monitor and manage Java applications
* `jdb` - find and fix bugs in Java platform programs
* `jdeprscan` - static analysis tool that scans a jar file (or some other aggregation of class files) for uses of deprecated API elements
* `jdeps` - launch the Java class dependency analyzer
* `jfr` - parse and print Flight Recorder files
* `jhsdb` - attach to a Java process or launch a postmortem debugger to analyze the content of a core dump from a crashed Java Virtual Machine (JVM)
* `jinfo` - generate Java configuration information for a specified Java process
* `jlink` - assemble and optimize a set of modules and their dependencies into a custom runtime image
* `jmap` - print details of a specified process
* `jmod` - create JMOD files and list the content of existing JMOD files
* `jpackage` - package a self-contained Java application
* `jps` - list the instrumented JVMs on the target system
* `jrunscript` - run a command-line script shell that supports interactive and batch modes
* `jshell` - interactively evaluate declarations, statements, and expressions of the Java programming language in a read-eval-print loop (REPL)
* `jstack` - print Java stack traces of Java threads for a specified Java process
* `jstat` - monitor JVM statistics
* `jstatd` - monitor the creation and termination of instrumented Java HotSpot VMs
* `jwebserver` - launch the Java Simple Web Server
* `keytool` - manage a keystore (database) of cryptographic keys, X.509 certificate chains, and trusted certificates
* `rmiregistry` - create and start a remote object registry on the specified port on the current host
* `serialver` - return the `serialVersionUID` for one or more classes in a form suitable for copying into an evolving class

**Windows Only**

* `jabswitch` - enable or disable Java Access Bridge
* `jaccessinspector` - examine accessible information about the objects in the Java Virtual Machine using the Java Accessibility Utilities API
* `jaccesswalker` - navigate through the component trees in a particular Java Virtual Machine and present the hierarchy in a tree view
* `javaw` - launch a Java application without a console window
* `kinit` - obtain and cache Kerberos ticket-granting tickets
* `klist` - display the entries in the local credentials cache and key table
* `ktab` - manage the principal names and service keys stored in a local key table

## An Example Project

```text
├───org.example
│       module-info.java
│
├───org.example.app
│   │   module-info.java
│   │
│   └───org
│       └───example
│           └───app
│                   Main.java
│
└───org.example.lib
    │   module-info.java
    │
    └───org
        └───example
            └───lib
                │   ExampleStringSupport.java
                │
                └───internal
                        EchoToolProvider.java
```

### javac

> Read Java class and interface definitions and compile them into bytecode and class files.

```shell
javac
  --module org.example,org.example.app,org.example.lib
  --module-source-path .
  -d .bach/out/classes
```

Here, compile 3 modules, namely...

- `org.example`,
- `org.example.app`, and
- `org.example.lib`.

Search source files in subdirectories of the current working directory `.`
named like the modules, and store class files in `.bach/out/classes`,
creating a subdirectory for each module

```text
├───org.example
│       module-info.class
│       
├───org.example.app
│   │   module-info.class
│   │   
│   └───org
│       └───example
│           └───app
│                   Main.class
│
└───org.example.lib
    │   module-info.class
    │   
    └───org
        └───example
            └───lib
                │   ExampleStringSupport.class
                │
                └───internal
                        EchoToolProvider.class
```

### jar

> Create an archive for classes and resources.

Three time's a charm, one call per module.

```shell
jar
  --create
  --file .bach/out/org.example.jar
  -C .bach/out/classes/org.example .
```

```shell
jar
  --create
  --file .bach/out/org.example.app.jar
  -C .bach/out/classes/org.example.app .
```

```shell
jar
  --create
  --file .bach/out/org.example.lib.jar
  -C .bach/out/classes/org.example.lib .
```

Here, create a modular JAR file named `org.example[.[app|lib]].jar`.

### jlink

```shell
jlink
  --verbose
  --output .bach/out/image
  --module-path .bach/out
  --add-modules org.example
  --launcher example=org.example.app/org.example.app.Main
```

### java

* Linux/Mac
  ```shell
  .bach/out/image/bin/example
  ```

* Windows
  ```shell
  .bach\out\image\bin\example
  ```

## Seven Steps to Find, Load, and Run Tools

### Step 0

Use `ToolProvider` (from `java.base/java.util.spi`) to find, load, and run `javac` tool.

```shell
java demo/Tool0.java
```

* Launch single-file source-code Java program (JEP 330)
* Run: `java demo/Tool0.java`
* TODO Pass non-empty main args arrays as tool's arguments
* Run: `java demo/Tool0.java --version`

```text
// === DONE ===
// [x] Used ToolProvider SPI
// [x] Ignored result of the tool execution

// === HINT ===
// [ ] args.length != 0 ? args : new String[] {"--version"}
// [ ] Run: java demo/Tool0.java --help-extra
// [ ] Run: java demo/Tool0.java --help-lint

// === NEXT ===
//  ?  Run: java demo/Tool0.java jar --version
// --> Transform into an application running an arbitrary tool
```

### Step 1

An application running an arbitrary tool.

```shell
java demo/Tool1.java jar --version
```

* Show usage message on empty args array
* Create `ToolRunner` interface with run method
* Move find and run code into default implementation
* TODO Print tool name and its args on run

```text
// DONE
// [x] Let args[0] be the name of the tool to run and args[1..n] its arguments
// [x] Added an abstraction for running a tool, throwing on non-zero exit code

// HINT
// [ ] System.out.println("// name = " + name + ", args = " + Arrays.deepToString(args));
// [ ] Run with well-known system tools: javac, jar, jlink

// NEXT
//  ?  Run with: jfr
// --> Implement a `--list-tools` option showing all observable tools and exit
// --> By introducing a configurable `ToolFinder` abstraction
```

### Step 2

Introduce ToolFinder to list observable tools.

* Create ToolFinder interface with abstract `List<ToolProvider> findAll()` method
* Add default `Optional<ToolProvider> find(NAME)` method to ToolFinder
* Add `ToolFinder.ofEmpty()` factory
* In `ToolRunner`, replace `ToolProvider.findFirst(NAME)` usage with `ToolFinder.find(NAME)`
* TODO Implement `ToolFinder.ofSystem()` by looking into `ToolProvider.findFirst(NAME)`

```shell
java demo/Tool2.java --list-tools
```

```text
// HINT:
// [x] Run: java --limit-modules java.base demo/Tool2.java

// NEXT:
//  ?  Run: java demo/Tool2.java banner hello world
// [ ] Implement a custom tool: `record Banner(String name) implements ToolProvider {...}`
// [ ] Implement a tool finder that accepts instances of `ToolProvider`
```

### Step 3

Local tool Banner and finder of tool instances.

```shell
java demo/Tool3.java banner hello world
```

```text
// Next step:
// [ ] Implement a tool finder that is composed of other finders
// [ ] Compose application's tool finder
```

### Step 4

Composing tool finders.

```shell
java demo/Tool4.java --list-tools
```

```text
// Next step:
// [ ] How to implement a tool that runs other tools?
// [ ] Add an abstraction for tool running tool: an operator
```

### Step 5

A tool operator runs other tools.

```shell
java demo/Tool5.java chain banner banner
```

```text
// Next step:
// [ ] Implement tool operators: Compile and Link
```

### Step 6

```shell
java demo/Tool6.java chain compile link
```

```text
// Next step:
// [ ] GOTO Tool.java
//     More ToolFinder...
// [ ] GOTO Bach.java
//     project-info!
```

## `Tool.java`

Putting it all together to access all tools the JDK provides.

```shell
java demo/Tool.java chain compile link run
```

* `ToolFinder.ofNativeTools(Path directory)`
  * Leveraging `ProcessBuilder` and `Process` API.
  * Example: `ofNativeTools(Path.of(System.getProperty("java.home"), "bin"))`

Explore more tool finders:

* `ToolFinder.ofJavaPrograms(Path directory)`
  * Single-File Source-Code Program `java Program.java ARGS...`
  * Executable JAR file `java -jar program.jar ARGS...`

* `ToolFinder.of(ModuleFinder)` and `ToolFinder.of(ModuleLayer)`
  * Example: `of(ModuleFinder.of(Path.of("out", "modules")))`

## `Bach.java`

_Want to see more?_

## Talk-Related Links

* JDK Tool Specifications
  <https://docs.oracle.com/en/java/javase/18/docs/specs/man>
* Project Jigsaw: Module System Quick-Start Guide
  <https://openjdk.java.net/projects/jigsaw/quick-start>
* Bach - Java Shell Builder - Builds (on(ly)) Modules
  <https://github.com/sormuras/bach>

## More Tool-Related Links

* List of Unix commands
  <https://en.wikipedia.org/wiki/List_of_Unix_commands>
* Unix philosophy
  <https://en.wikipedia.org/wiki/Unix_philosophy>
* JEP 293: Guidelines for JDK Command-Line Tool Options
  <https://openjdk.java.net/jeps/293>
* JDK-8275072: Enhance java.util.spi.ToolProvider
  <https://bugs.openjdk.java.net/browse/JDK-8275072>
* ARGument harVESTER - a simple command line parser written with Love and Java 15
  <https://github.com/forax/argvester>

## More Tools, More Options, More Examples

### jpackage

### jtreg (7+)

### jextract (?)

### junit (external)

### jreleaser (external)
