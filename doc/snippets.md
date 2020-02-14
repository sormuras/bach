# Java Shell Builder

Ideas, thoughts, and more on the architecture of `Bach.java`.

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

## Directory Layout

### Simplistic

`src/${MODULE}/module-info.java`

- `--module-source-path src`

### With Realms

`src/${MODULE}/${REALM}/java/module-info.java`

- `--module-source-path src/*/main/java`
- `--module-source-path src/*/test/java`


### With Groups And Realms

`src/${GROUP}/${MODULE}/${REALM}/java/module-info.java`

- `--module-source-path src/org.foo/*/main/java:src/org.bar/*/main/java:...`
- `--module-source-path src/org.foo/*/test/java:src/org.bar/*/test/java:...`

## Library

This section describes how directory `lib/` is populated by `${MODULE}-${VERSION}.jar` modules.

### Resolve recursively

Or what is flood-fill?

1. Populate `lib/` directory manually.
1. Resolve modules declared via `library.requires` feature.
1. Resolve all missing `requires ${MODULE}` declared in **project**'s modules.
1. Resolve all missing `requires ${MODULE}` declared in **library**'s modules.
1. **Flood-fill** library by repeating previous step until no required module is missing.

#### Manual modules

Download any modular JAR file and drop it into the `lib/` directory of the project.

#### Library requires

Programmatically via: `new Library(List.of("org.junit.jupiter[@5.6.0]"[, ...]), ...)`

Via configuring a comma-separated list of module names: `library.requires=org.junit.jupiter[@5.6.0][, ...]`

### Resolve single `requires ${MODULE}`

Optional dependence, i.e. `requires static ${MODULE}` are not resolved. Use

Algorithm outline: 

1. Pre-defined Links

   - JavaFX
   - JUnit Platform and Jupiter
   - OpenTest4J
   - APIGuardian

1. User-defined *module-name-to-uri* Links

   - `module/<name>@<default version>=https://repo1.maven.org/maven2/.../<artifactId>-${VERSION}.jar`

1. Fall back to `sormuras/modules` based resolution

    1. User-defined "module version" beats `module-version.properties`
    1. User-defined "module group:artifact" over `module-maven.properties`

1. Throw `UnmappedModuleException`

## Wish List

- Support ZIP files as sources of `Project.BuilderFactory`

## Workshop Description

In this workshop you learn how to use Bach.java, a lightweight build tool for Java.
No tricks or foreign syntax required: no x(ml), no y(aml), no z... just *.java files.

Bach.java uses jshell/java to build modular Java projects.
It supports a zero-installation run mode, convention over configuration pragmatism covers most default values, allows property-based overrides to tweak some default values, and a clean Java API to customize your build.
This allows stepping through the build process with a debugger. But that's rarely needed.

Bach.java is targeted to coders of small to mid-size Java projects, who want to focus on their ideas and modules instead of learning and taming a build tool.
In the spirit of Rémi Forax, who wrote: _"No need to be a maven to be able to use a build tool"_.
A build tool should just shuffle the JDK foundtation tool in the right order and pass the right arguments.
Nothing more, noting less.
That's what Bach.java does for you.
From `javac` over `jar` to `jlink`/`jpackage`.
