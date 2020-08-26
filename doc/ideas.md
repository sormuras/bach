## Design Ideas

```text
Basic Shell   Project Descriptor      Project Builder
-----------   ------------------      ---------------
  mkdir         .name()                 .build() { compile + test + pack }
  rmdir         .version()              .compile() { compile(main, test) }
  tree          .modules()              .test() { test("module") + junit }
  run(...)      .testModules()          .pack() { api + image + poms + X }
```

### Basic Shell API

Basic tool running framework.

```java
var bach = new Bach();
bach.run("javac", "--version", ...);

var javac = new Javac().printVersion();
bach.run(javac);

var javadoc = new Javadoc().printVersion();
bach.run(javac, javadoc, ...); // Structured Concurrency
```

### Project Descriptor

Describes modular Java projects.

```java
new ProjectDescriptor()
  .name("greet")
  .version("2")
  .compileForJavaRelease(8)
  .moduleSourcePath("src", "*", "main", "java");
  .module("com.greetings")
  .module("org.astro")
  .testModuleSourcePath("src", "*", "test", "java")
  .testModule("test.base")
  ...;
```

### Project Builder API

An extensible framework building projects.

```java
var bach = new Bach().printer(System.out::println);

var project = new ProjectDescriptor().name("greet").version("2");

var builder = new ProjectBuilder(bach, project) {
  @Override
  public Javac computeJavacCallForCompileModules() {
    return super.computeJavacCall().printMessagesAboutTheCompilerIsDoing();
  }

  @Override
  public Javac computeJavacCallForCompileTestModules() {
    return super.computeJavacCall().generateAllDebuggingInfo();
  }
};

try (builder) {
  builder.deleteClassesDirectories();
  builder.compileModules();
  builder.compileTestModules();
  builder.executeTestModules();
  builder.run(
    builder::generateApiDocumenation,
    builder::generateCustomRuntimeImage,
    builder::generateMavenPomFiles,
    ...
  );
} finally {
  builder.writeLogbook();
}
```
