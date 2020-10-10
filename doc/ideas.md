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

### Project API

Describes modular Java projects.

```java
var main = new MainCodespace()
    .moduleSourcePath("src/*/main/java")
    .modules("com.greetings", "org.astro")
    .compileForJavaRelease(8)
    .includeSourceFilesInModules(true)
    ...;
```

```java
var test = new TestCodespace("test", main)
    .moduleSourcePath("src/*/test/java")
    .modules("test.base", "org.astro")
    .allowClassesToDependOnPreviewFeatures(false)
    ...;
```

```java
var preview = new TestCodespace("test-preview", main, test)
    .moduleSourcePath("src/*/test-preview/java")
    .modules("test.preview")
    .allowClassesToDependOnPreviewFeatures(true)
    ...;
```

```java
var project = new ProjectDescriptor("greet", 2)
    .codespaces(main, test, preview)
    .requiresJavaDevelopmentKit(15, /* allowHigherJavaVersions */ true)
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
    return super.computeJavacCallForCompileModules().printMessagesAboutWhatTheCompilerIsDoing();
  }

  @Override
  public Javac computeJavacCallForCompileTestModules() {
    return super.computeJavacCallForCompileTestModules().generateAllDebuggingInfo();
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
