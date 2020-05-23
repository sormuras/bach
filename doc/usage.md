## Usage

Two distinct "Run Modes" are available.

### Single-file source-code API

`Bach.java` as `Bach.java`, literally.

#### Zero-installation build via `jshell <load-file>`.

Let `jshell` load, compile, and run it.

  - `load-file` = `https://github.com/sormuras/bach/raw/master/src/bach/bach-build.jsh`
  - `load-file` = `https://sormuras.de/bach-build`

#### Run a local installment of `Bach.java`

Store a copy of `Bach.java` in your project.

  - `jshell https://sormuras.de/bach-boot`

Offers IDE support with writing your `Build.java` program.
Namespace uses `Bach.` prefix.

Run your build program via

  - `.bach/build`

### Module `de.sormuras.bach` API

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

Bach.of(project).build().assertSuccessful();
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
