## Project Descriptor

_The right tools with the right arguments at the right time._

```java
project greet {
  version 1-ea;
  modules com.greetings,org.astro;
  module-source-path "src/*/main/java"; 
  main-module com.greetings;
  target-jvm-release 8;
  resolve-external-module ${module} via "https....jar";

  test-modules test.base,test.integration;
  test-module-source-path "src/*/test/java";
}
```

A project descriptor declares properties used for building a modular Java project.

- `project-info.java`
  - `project.name` is used for `javadoc`'s archive name and `jlink`'s launcher
  - `project.type` one of `[ANY, APPLICATION, LIBRARY]` to toggle default tool activation states
  - `project.version` is used as the default value for all main modules
  - `project.locates` associates a module name with a uniform resource locator
  - `project.requires` is used for declaring additional external module dependences

A project descriptor contains exactly one main module set.

- `MainModuleSet`
  - `main.units` list of one or more main module units
  - `main.tool` tool activation flag and call arguments
    - `main.tool.javac` compile `main.units` to `.class` files and calls `unit.tool.jar` for each unit
    - `main.tool.javadoc` generate API documentation
    - `main.tool.jar-javadoc` copy generated web page into `${project.name}-${project.version}-api.jar`
    - `main.tool.jlink` create a custom runtime image
    - `main.tool.jpackage` create a Java runtime package

The main module set contains one or more module units.
A module unit is an extended module description that is based on a regular module descriptor.

- `ModuleUnit`
  - `unit.module` is parsed from `module-info.java` compilation unit
    - `unit.module.name` is enqueued to the list of modules to be compiled
    - `unit.module.requires` is used for determining external modules
  - `unit.sources` lists directories with Java source files
  - `unit.resources` lists all resource directories
  - `unit.tool` tool activation flag and call arguments
    - `unit.tool.jar` copy `.class` and resources into `${unit.module.name}@${project.version}.jar`
    - `unit.tool.jar-sources` copy `.java` files into `${unit.module.name}@${project.version}-sources.jar`

### Main Module Set

- cardinality: 1
- default name: `"main"`
- name aliases: `""`, `"core"`

#### Compile Main Modules

- `javac` for main module set
- `jar` for each main module unit

#### Generate API Documentation

- `javadoc` for main module set
- `jar` HTML files

#### Create Custom Runtime Image

- `jlink` all main modular JAR file

#### Create Java Runtime Package

- `jpackage` custom runtime image

#### Create Native Application

- _`jnative`_ via Project Leyden

### Test Module Sets

- cardinality: 0..n
- names: `"test"`, `"test-preview"`, ...

#### Compile Test Modules

- `javac` for all test modules
- `jar` for each test module

#### Run JUnit Platform

- `junit` for each test module
