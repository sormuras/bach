# Java Shell Builder

Ideas, thoughts, and more on the architecture of `Bach.java`.

## Uniques

- zero installation required (besides JDK 11+, using `jshell`)
- zero extra configuration required (conventions and information gathered from `module-info.java`)
- easy to customize using plain Java (no other programming language, perhaps `.properties` files to define hints)
- 3rd-party modules in plain sight (single `lib/` directory)
- considers compile (`javac`) and package (`jar`) as an atomic step
- single-pass multi-module processing (`--module-source-path`)
- multi-release modules (`java-7`, `java-8`, ..., `java-11`, ..., `java-N`)
- automated checks (`test(${MODULE})`,`junit`)
- document (`${MODULE}-javadoc.jar`, `${MODULE}-sources.jar`)

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
  - Stored in project as `lib/de.sormuras.bach-{VERSION}.jar`, it is...
    - runnable via `java -p lib -m de.sormuras.bach action [args]`
    - mount- and runnable within an IDE
  - Customizable by...
    - implement `module build {requires de.sormuras.bach}` and provide custom project instance
      and services
  - Types can be merged to a single-file source-code `Bach.java` program via `Merge.java` to
    support a similar setup
