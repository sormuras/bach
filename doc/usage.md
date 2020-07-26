## Usage

This document describes usage of Bach.java in two parts.
First, the JShell-based on-the-fly usage modes "boot" and "build" are described.
Bridged via the "init" script, the second part introduces the basic usage of module `de.sormuras.bach` API.

### Boot into JShell with Bach

Launch `jshell` with Bach.java loaded and mounted in interactive mode.

  - `load-file` = `https://sormuras.de/bach/boot`
  - `load-file` = `https://github.com/sormuras/bach/raw/HEAD/src/bach/bach-boot.jsh`

A specific version, here 11.6, of Bach.java is loaded via the to following load files.

  - `load-file` = `https://sormuras.de/bach@11.6/boot`
  - `load-file` = `https://github.com/sormuras/bach/raw/11.6/src/bach/bach-boot.jsh`

This version pattern is applicable to all JShell scripts.

  - `https://sormuras.de/bach[@${VERSION}]/${NAME}` expands to
  - `load-file` = `https://github.com/sormuras/bach/raw/${VERSION}/src/bach/bach-${NAME}.jsh`

### Zero-installation build via `jshell <load-file>`.

Let `jshell` load and run Bach.java to build your project.

  - `load-file` = `https://github.com/sormuras/bach/raw/HEAD/src/bach/bach-build.jsh`
  - `load-file` = `https://sormuras.de/bach/build`

### Module `de.sormuras.bach` API

```
├───.bach
│   │   .gitignore
│   ├───lib
│   │       de.sormuras.bach@${VERSION}.jar
│   └───src
│       └───build
│           │   module-info.java
│           └───build
│                   Build.java
├───lib
│       3rd-party-module.jar
└───src
    └───com.greetings
            module-info.java
```

Mount modular `de.sormuras.bach@${VERSION}.jar`.
Use its API directly in your modular build program.
With internal packages hidden.

### API

#### Bach API

```java
var bach = new Bach(/*project, http client supplier, ...*/);

bach.build();
// TODO bach.clean();
// TODO bach.erase();
// TODO bach.help();
// TODO bach.info();
```

#### Creating a new Project instance

##### Project

```java
var project = new Project(
        new Base(/*folders, files, ...*/),
        new Info(/*title, version, ...*/)
        // here be more immutable component values...
    );

Bach.of(project).build();
```

##### Project Builder

A `Builder` collects custom components and creates a `Project` instance.
A `Builder` provides convenient setters accepting basic types: e.g. `String` instead of `Path`.

```java
var builder = new Project.Builder()
    .title("Here be dragons...")
    .version("47.11");

var project = builder.newProject();

Bach.of(project).build().assertSuccessful();
```

##### Project Builder Builder

A `Scanner` parses a directory and creates a `Builder` instance -- a builder builder.
A `Scanner` is customizable like a builder.

```java
var scanner = new Scanner()
    .base(Path.of(""))
    .layout(...)
    .limit(5);

var builder = scanner.newBuilder()
    .title("Here be dragons...")
    .version("47.11");

var project = builder.newProject();

Bach.of(project).build().assertSuccessful();
```
