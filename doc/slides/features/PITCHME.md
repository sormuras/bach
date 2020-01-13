---?image=https://upload.wikimedia.org/wikipedia/commons/0/0e/Bach_Seal.svg&size=cover&opacity=5
@snap[north span-90]
## Features
@snapend
@snap[midpoint span-90]
@ul[list-no-bullets](false)
- Intro ✔
- Demo ✔
- **Features**
- @css[text-gray](Model)
- @css[text-gray](Outlook)
@ulend
@snapend

+++
@snap[north span-90]
### Features
@snapend
@snap[west span-40]
@ul[list-no-bullets](false)
- **zero installation** besides&nbsp;JDK
- **zero configuration** conventions
- **b-y-o-b** Java&nbsp;program
- **3rd-party modules** in&nbsp;plain&nbsp;sight
@ulend
@snapend
@snap[east span-40]
@ul[list-no-bullets](false)
- **compilation** `javac + jar`
- **multi-module** single&nbsp;pass
- **multi-release** modules
- **automated checks** built-in
@ulend
@snapend
@snap[south span-100 text-07 text-blue]
Bach.java - Features
@snapend
Note:
- besides JDK 11+, using `jshell`
- conventions and information gathered from `module-info.java` files
- using plain old Java -- `src/bach/Build.java`
- single `lib/` directory
- compile and package as an atomic step
- `--module-source-path`, `${PROJECT.NAME}-javadoc.jar`
- `java-7`, `java-8`, ..., `java-11`, ..., `java-N`
- `test(${MODULE})`,`junit`

+++
@snap[north span-90]
### Zero Installation
@snapend
@snap[midpoint span-90 text-06]
- `jshell https://bit.ly/`**bach-build**
  Downloads Bach.java, and builds project in the current user directory.
- `jshell https://bit.ly/`**bach-init**
  Downloads Bach.java, creates launch scripts and prints project information.
@snapend
@snap[south span-100 text-07 text-blue]
Bach.java - Features - Zero Installation
@snapend
Note:
- Enter the base directory of your Java project, open a shell, and execute the [bach-build] command:
- Long `jshell https://raw.githubusercontent.com/sormuras/bach/master/src/bach/build.jsh`
- That's it.

+++
@snap[north span-90]
### Zero Configuration
@snapend
@snap[midpoint span-90 text-06]
Almost all required information to build a modular Java project is either deduced from conventions or gathered from
module declarations, i.e. `module-info.java` files.

- Required modules' versions via https://github.com/sormuras/modules

Also, the following attributes are extracted from comments (soon annotations?) found in module declarations: 

- Module version `--module-version ...`
- Module entry-point `--main-class ...`
@snapend
@snap[south span-100 text-07 text-blue]
Bach.java - Features - Zero Configuration
@snapend

+++
@snap[north span-90]
### Bring Your Own Build
@snapend
@snap[midpoint span-90 text-06]
- `src/bach/Build.java`
- Write build program using plain old Java

```
public static void main(String... args) {
  System.out.println("Building my own project...");
  Bach.build(
    Configuration.of("project", Version.parse("1.2.3"))
        .setLog(Log.ofSystem(true))
        .setGroup("my.own.project"));
}
```
@snapend
@snap[south span-100 text-07 text-blue]
Bach.java - Features - Bring Your Own Build
@snapend

+++
@snap[north span-90]
### 3rd-party modules
@snapend
@snap[midpoint span-90 text-06]
All 3rd-party modules are stored in plain sight: `lib/`

How do you provide 3rd-party modules? Load and drop modular JAR files into the `lib/` directory.
Missing 3rd-party modules are being resolved in a best-effort manner using [sormuras/modules](https://github.com/sormuras/modules) database.
@snapend
@snap[south span-100 text-07 text-blue]
Bach.java - Features - 3rd-party modules
@snapend
Note: 3rd-party modules are all modules that are not declared in your project and that are not modules provided by the _system_, i.e. the current Java runtime.

+++
@snap[north span-90]
### Compilation, Multi-Module, Multi-Release
@snapend
@snap[midpoint span-90 text-06]
- `javac` + `jar`

Exploded class files are only a intermediate state.
This ensures, that at test runtime, you're checking your modules as if they are already published.
Including loading services and resources.

Using the `--module-source-path` option from `javac` all modules are compiled in a single pass.
With the exception multi-release modules - they are build before any other module.

Organize your Java sources in targeted directories to create a multi-release JAR.

@snapend
@snap[south span-100 text-07 text-blue]
Bach.java - Features - Compilation, Multi-Module, Multi-Release
@snapend

+++
@snap[north span-90]
### Automated Checks
@snapend
@snap[midpoint span-90 text-06]
Two kinds of automated checks per module are supported:

- Provided tool named `test(${MODULE})`
- JUnit Platform with selecting the current module under test

@snapend
@snap[south span-100 text-07 text-blue]
Bach.java - Features - Automated Checks
@snapend
