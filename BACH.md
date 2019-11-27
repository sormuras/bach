# Java Shell Builder

Ideas, thoughts, and more on the architecture of `Bach.java`.

## Uniques

- [x] [zero installation](#zero-installation) required (besides JDK 11+, using `jshell`)
- [x] [zero extra configuration](#zero-extra-configuration) required (conventions and information gathered from `module-info.java` files)
- [x] [customize with properties](#customize-via-properties) to override auto-configured values (`.bach/project.properties`)
- [x] [b-y-o-b](#bring-your-own-build) program using plain old Java (`src/bach/Build.java`)
- [ ] 3rd-party modules in plain sight (single `lib/` directory)
- [ ] considers compile (`javac`) and package (`jar`) as an atomic step
- [ ] single-pass multi-module processing (`--module-source-path`)
- [ ] multi-release modules (`java-7`, `java-8`, ..., `java-11`, ..., `java-N`)
- [ ] automated checks (`test(${MODULE})`,`junit`)
- [ ] document (`${MODULE}-javadoc.jar`, `${MODULE}-sources.jar`)

## Zero Installation

Enter the base directory of your Java project, open a shell, and execute one of the following commands:

- Long: `jshell https://raw.githubusercontent.com/sormuras/bach/master/src/bach/Bach.jsh`
- Shorter: `jshell https://github.com/sormuras/bach/raw/master/src/bach/Bach.jsh`
- Shortened: `jshell https://bit.ly/bach-jsh`

That's it.

## Zero Extra Configuration

Almost all required information to build a modular Java project is either deduced from conventions or gathered from
module declarations, i.e. `module-info.java` files.

- Required modules' versions via https://github.com/sormuras/modules

Also, the following attributes are extracted from comments (soon annotations?) found in module declarations: 

- Module version `--module-version ...`
- Module entry-point `--main-class ...`

## Customize Via Properties

The default [ProjectBuilder](https://github.com/sormuras/bach/blob/master/src/de.sormuras.bach/main/java/de/sormuras/bach/project/ProjectBuilder.java)
implementation loads some properties from a `.bach/project.properties` file.
Here, you may set different values for project properties like `name`, `version`, etc.

## Bring Your Own Build

Does your project use a different structure?
Want to execute tasks is a different order?
Need additional tasks to be executed?
Want skip tests?

Write your own build program using plain old Java!

Store your build program as [`src/bach/Build.java`](https://github.com/sormuras/bach/blob/master/src/bach/Build.java) and [Bach.java](https://github.com/sormuras/bach/blob/master/src/bach/Bach.java#L43) will delegate to it.
To make your own build program runnable from within an IDE, you need to download and mount module `de.sormuras.bach` first.
After that simply run the `de.sormuras.bach` module.

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
