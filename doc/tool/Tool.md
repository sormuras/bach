# JDK Tools and Where to Find Them

Java libraries and applications can be built using tools the Java Development Kit (JDK) provides.

This tutorial starts with an overview of 28 tools shipping with JDK 18.
A few of them are executed on the command-line in the right order and with the right arguments to build an example application.
In order to execute command-line tools programmatically, the `ToolProvider` interface providing a way invoke tools without necessarily starting a new VM was introduced in Java 9.
Leveraging the "Launch Single-File Source-Code Programs" feature of JDK Enhancement Proposal (JEP) 330 a standalone tool-running program is developed. 

## Tools, Options, Examples

The "Java Platform, Standard Edition & Java Development Kit Specifications Version 18" 

### Java Development Kit Version 18 Tool Specifications

All Platforms

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

### javac

```shell
javac
 -d .bach/out/classes
 --module-source-path .
 --module org.example
```

### javadoc

```shell
javadoc
 -d .bach/out/javadoc
 --module-source-path .
 --module org.example
```

### jar

```shell
jar
  --create
  --file .bach/out/org.example.jar
  --main-class org.example.Main
  -C .bach/out/classes/org.example .
```

### jlink

```shell
jlink
 --output .bach/out/image
 --module-path .bach/out
 --add-modules org.example
 --launcher example=org.example
```

* Linux/Mac
  ```shell
  .bach/out/image/bin/example
  ```

* Windows
  ```shell
  .bach\out\image\bin\example
  ```

## Find, Load, and Run Provided Tool

### `ToolProvider` SPI

An interface for command-line tools to provide a way to be invoked without necessarily starting a new VM.

### `ToolProvider` via `ServiceLoader`

```java
package java.util.spi;

interface ToolProvider {
  static Optional<ToolProvider> findFirst(String name) {
    // ...
  }
}
```

Returns the first instance of a ToolProvider with the given name, as loaded by ServiceLoader using the system class loader.

## `Tool` API and `Tool.Finder` SPI

> `Tool.Finder` is to `Tool` is what `ModuleFinder` is to `ModuleReference`.

### `Tool` API

```java
/**
 * A tool reference.
 */
record Tool(String name, java.util.spi.ToolProvider provider) {}
```

### Introduce `Tool.Finder`

```java
import java.util.List;
import java.util.Optional;
import java.util.spi.ToolProvider;

record Tool(String name, ToolProvider provider) {

  interface Finder {

    List<Tool> findAll();

    default Optional<Tool> find(String name) {
      return findAll().stream().filter(tool -> tool.name().equals(name)).findFirst();
    }
  }
}
```

```java
record Tool(String name, ToolProvider provider) {

  static Tool of(ToolProvider provider) {
    return new Tool(provider.name(), provider);
  }

}
```

### `ToolFinder.of(Tool...)`

Direct tool finder.

```java
record Tool(String name, ToolProvider provider) {

  static ToolFinder of(Tool... tools) {
    record DirectToolFinder(List<Tool> findAll) implements ToolFinder {}
    return new DirectToolFinder(List.of(tools));
  }

}
```

### `ToolFinder.ofSystemTools()`

### `ToolFinder.ofModuleFinder()`

### `ToolFinder.ofNativeTools()`

* `ProcessBuilder`
* `Process`

### `ToolFinder.ofNativeJavaHomeTools()`

* `java[.exe]` in `${JAVA_HOME}/bin`|`%JAVA_HOME%\bin`

## Run tools from within a tool run

More precise: how to run non-system tools from within a non-system tool run method?

### `ToolProvider`

```java
class SomeToolProvider implements ToolProvider {
    
  public String name() { return "some"; }

  public int run(PrintWriter out, PrintWriter err, String... args) {
    ToolProvider.findFirst("hello").orElseThrow().run(out, err, "world");
    ToolProvider.findFirst("banner").orElseThrow().run(out, err, "text");
  }

}
```

* No context.
* No tool finder.

### `ToIntBiFunction<T, U>`

Implement `ToolProvider` and, ignoring custom out and err parameters, `ToIntBiFunction<BiConsumer<String, List<String>, List<String>>>`.
Using well-known Java types and relying on the calles to pass-in a suitable runner instance.

```java
class SomeToolProvider implements
          ToolProvider,
          ToIntBiFunction<BiConsumer<String, List<String>, List<String>>> {

  public String name() { return "some"; }

  public int applyAsInt(BiConsumer<String, List<String>> runner, List<String> args) {
    runner.accept("hello", List.of("world"));
    runner.accept("banner", List.of("text"));
  }

}
```

* Well...

### `ToolRunner` SPI

```java
record Tool(String name, ToolProvider provider) {

  interface Runner {
    int run(PrintWriter out, PrintWriter err, String name, String... args);
  }

  interface Provider extends ToolProvider {
    int run(Runner runner, PrintWriter out, PrintWriter err, String name, String... args);
  }

}
```

```java
class SomeToolProvider implements ToolProvider {

  public String name() { return "some"; }

  public int run(ToolRunner runner, PrintWriter out, PrintWriter err, String... args) {
    runner.run(out, err, "hello", "world");
    runner.run(out, err, "banner", "text");
  }

}
```

## `Bach.java`

**TODO**

```shell
bach --project-version 123 build
```

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
* ARGument harVESTER - a simple command line parser written with Love and Java 15
  <https://github.com/forax/argvester>

## More Tools, More Options, More Examples

### jpackage

### jreleaser (external)
