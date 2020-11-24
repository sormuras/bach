package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.Paths;
import com.github.sormuras.bach.module.ModuleDirectory;
import com.github.sormuras.bach.module.ModuleSearcher;
import com.github.sormuras.bach.project.MainSpace;
import com.github.sormuras.bach.project.TestSpace;
import com.github.sormuras.bach.tool.Command;
import com.github.sormuras.bach.tool.ToolCall;
import com.github.sormuras.bach.tool.ToolResponse;
import com.github.sormuras.bach.tool.ToolRunner;
import java.io.File;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Builds a modular Java project. */
public class ProjectBuilder {

  private final Bach bach;
  private final Project project;
  private final ModuleDirectory moduleDirectory;
  private final ToolRunner runner;

  /**
   * Initialize this project builder with the given components.
   *
   * @param bach the {@code Bach} instance
   * @param project the project to build
   */
  public ProjectBuilder(Bach bach, Project project) {
    this.bach = bach;
    this.project = project;
    this.moduleDirectory = ModuleDirectory.of(Project.LIBRARIES, project.library().links());
    this.runner = new ToolRunner(moduleDirectory.finder());
  }

  void info(String format, Object... args) {
    bach.logbook().log(Level.INFO, format, args);
  }

  void run(ToolCall call) {
    info(call.toCommand().toString());
    var response = runner.run(call);
    bach.logbook().log(response);
    response.checkSuccessful();
  }

  /** Builds a modular Java project. */
  public void build() {
    var start = Instant.now();
    var logbook = bach.logbook();
    info("Build project %s %s", project.name(), project.version());
    try {
      if (project.findAllModuleNames().count() == 0) throw new RuntimeException("No module found!");
      loadRequiredAndMissingModules();
      build(project.main());
      build(project.test());
    } catch (Exception exception) {
      logbook.log(Level.ERROR, exception.toString());
      throw new BuildException("Build failed: " + exception);
    } finally {
      info("Build took %s", Logbook.toString(Duration.between(start, Instant.now())));
      var file = logbook.write(project);
      logbook.accept("Logbook written to " + file.toUri());
    }
  }

  /** Load required and missing modules in a best-effort manner. */
  public void loadRequiredAndMissingModules() {
    var searchers = new ArrayList<>(project.library().searchers());
    searchers.add(ModuleSearcher.ofBestEffort(bach));
    var searcher = ModuleSearcher.compose(searchers.toArray(ModuleSearcher[]::new));
    var requires = project.library().requires();
    requires.forEach(module -> bach.loadModule(moduleDirectory, searcher, module));
    bach.loadMissingModules(moduleDirectory, searcher);
  }

  /**
   * Builds the main space.
   *
   * <ul>
   *   <li>javac + jar
   *   <li>javadoc
   *   <li>jlink
   *   <li>jpackage
   * </ul>
   *
   * @param main the main space to build
   */
  public void build(MainSpace main) {
    if (main.modules().isEmpty()) return;
    Paths.deleteDirectories(main.workspace("modules"));

    info("Compile main modules");
    if (main.release() >= 9) run(computeMainJavacCall());
    else {
      var feature = Runtime.version().feature();
      run(computeMainJavacCall(feature));
      var libraries = Project.LIBRARIES;
      var classPaths = new ArrayList<Path>();
      for (var module : project.main().modules()) classPaths.add(main.classes(feature, module));
      if (Files.isDirectory(libraries)) classPaths.addAll(Paths.list(libraries, Paths::isJarFile));
      buildSingleRelease78Modules(classPaths);
    }

    Paths.createDirectories(main.workspace("modules"));
    for (var module : project.main().modules()) {
      for (var release : project.main().supplements().get(module).releases()) {
        run(computeMainJavacCall(module, release));
      }
      run(computeMainJarCall(module));
    }

    if (isGenerateApiDocumentation()) {
      info("Generate API documentation");
      run(computeMainDocumentationJavadocCall());
      run(computeMainDocumentationJarCall());
    }

    if (isGenerateCustomRuntimeImage()) {
      info("Generate custom runtime image");
      Paths.deleteDirectories(main.workspace("image"));
      run(computeMainJLinkCall());
    }

    if (isGenerateApplicationPackage()) {
      info("Generate self-contained Java application");
      run(computeMainJPackageCall());
    }
  }

  /**
   * Builds all main modules in a single.
   *
   * @param classPaths paths to pre-compiled classes and external modules
   */
  public void buildSingleRelease78Modules(List<Path> classPaths) {
    var main = project.main();
    if (main.release() > 8) throw new IllegalStateException("release too high: " + main.release());

    for (var module : project.main().modules()) {
      var moduleInfoJavaFiles = new ArrayList<Path>();
      Paths.find(Path.of(module, "main/java-module"), "module-info.java", moduleInfoJavaFiles::add);
      var compileModuleOnly =
          Command.builder("javac")
              .with("--release", 9)
              .with("--module-version", project.version())
              .with("--module-source-path", String.join(File.pathSeparator, main.moduleSourcePaths()))
              .with("--module-path", String.join(File.pathSeparator, main.modulePaths()))
              .with("-implicit:none") // generate classes for explicitly referenced source files
              .withEach(main.tweaks().getOrDefault("javac", List.of()))
              .with("-d", main.classes())
              .withEach(moduleInfoJavaFiles)
              .build();
      run(compileModuleOnly);

      var path = Path.of(module, "main/java");
      if (Files.notExists(path)) continue;

      var javaSourceFiles = new ArrayList<Path>();
      Paths.find(path, "**.java", javaSourceFiles::add);
      var javac =
          Command.builder("javac")
              .with("--release", main.release()) // 7 or 8
              .with("--class-path", Paths.join(classPaths))
              .withEach(main.tweaks().getOrDefault("javac", List.of()))
              .with("-d", main.classes().resolve(module))
              .withEach(javaSourceFiles);
      run(javac.build());
    }
  }

  /**
   * Builds the test space.
   *
   * <ul>
   *   <li>javac + jar
   *   <li>junit
   * </ul>
   *
   * @param test the test space to build
   */
  public void build(TestSpace test) {
    if (test.modules().isEmpty()) return;
    Paths.deleteDirectories(test.workspace("modules-test"));

    info("Compile test modules");
    run(computeTestJavacCall());
    Paths.createDirectories(test.workspace("modules-test"));
    for (var module : project.test().modules()) {
      run(computeTestJarCall(module));
    }

    if (moduleDirectory.finder().find("org.junit.platform.console").isPresent()) {
      for (var module : project.test().modules()) {
        var archive = module + "@" + project.version() + "+test.jar";
        var finder =
            ModuleFinder.of(
                test.workspace("modules-test", archive), // module under test
                test.workspace("modules"), // main modules
                test.workspace("modules-test"), // (more) test modules
                Project.LIBRARIES // external modules
                );
        info("Launch JUnit Platform for test module: %s", module);
        var junit = computeTestJUnitCall(module);
        info(junit.toCommand().toString());
        var response = runner.run(junit, finder, module);
        bach.logbook().log(response);
      }
      var errors = bach.logbook().responses(ToolResponse::isError);
      if (errors.size() > 0) {
        throw new BuildException("JUnit reported failed test module(s): " + errors.size());
      }
    }
  }

  /** @return {@code true} if an API documenation should be generated, else {@code false} */
  public boolean isGenerateApiDocumentation() {
    return project.main().generateApiDocumentation();
  }

  /** @return {@code true} if a custom runtime image should be generated, else {@code false} */
  public boolean isGenerateCustomRuntimeImage() {
    return project.main().generateCustomRuntimeImage();
  }

  /** @return {@code true} if a self-contained package should be generated, else {@code false} */
  public boolean isGenerateApplicationPackage() {
    return project.main().generateApplicationPackage();
  }

  /** @return the {@code javac} call to compile all modules of the main space. */
  public ToolCall computeMainJavacCall() {
    return computeMainJavacCall(project.main().release());
  }

  /**
   * @param release the release
   * @return the {@code javac} call to compile all modules of the main space
   */
  public ToolCall computeMainJavacCall(int release) {
    var main = project.main();
    return Command.builder("javac")
        .with("--release", release)
        .with("--module", String.join(",", main.modules()))
        .with("--module-version", project.version())
        .with("--module-source-path", String.join(File.pathSeparator, main.moduleSourcePaths()))
        .withEach(main.tweaks().getOrDefault("javac", List.of()))
        .with("-d", main.classes(release))
        .build();
  }

  /**
   * @param module the module to compile
   * @param release the release to compile
   * @return the {@code javac} call to compile a version of a multi-release module
   */
  public ToolCall computeMainJavacCall(String module, int release) {
    var main = project.main();
    var classes = main.workspace("classes-mr", release + "/" + module);
    var javaSourceFiles = new ArrayList<Path>();
    Paths.find(Path.of(module, "main/java-" + release), "**.java", javaSourceFiles::add);
    return Command.builder("javac")
        .with("--release", release)
        .with("--module-version", project.version())
        .with("--module-path", main.classes())
        .with("-implicit:none") // generate classes for explicitly referenced source files
        .withEach(main.tweaks().getOrDefault("javac", List.of()))
        .with("-d", classes)
        .withEach(javaSourceFiles)
        .build();
  }

  /**
   * @param module the name of module to create an archive for
   * @return the {@code jar} call to archive all assets for the given module
   */
  public ToolCall computeMainJarCall(String module) {
    var main = project.main();
    var archive = computeMainJarFileName(module);
    var jar =
        Command.builder("jar")
            .with("--create")
            .with("--file", main.workspace("modules", archive))
            .withEach(main.tweaks().getOrDefault("jar", List.of()))
            .withEach(main.tweaks().getOrDefault("jar(" + module + ')', List.of()))
            .with("-C", main.classes().resolve(module), ".");
    // include resource and source files
    var supplement = main.supplements().get(module);
    var info0 = supplement.info();
    var source0 = info0.getParent();
    if (source0 == null) jar.with(info0);
    else {
      jar.with("-C", source0, ".");
      if (Paths.name(source0).equals("java-module")) {
        var java0 = source0.resolveSibling("java");
        if (Files.isDirectory(java0)) jar.with("-C", java0, ".");
      }
    }
    // add versioned class files
    for (var release : supplement.releases()) {
      var classes = main.workspace("classes-mr", release + "/" + module);
      jar.with("--release", release).with("-C", classes, ".");
      // add versioned resource and source files
      if (source0 != null) {
        var sources = source0.resolveSibling("java-" + release);
        jar.with("-C", sources, ".");
      }
    }
    return jar.build();
  }

  /**
   * @param module the name of the module
   * @return the name of the JAR file for the given module
   */
  public String computeMainJarFileName(String module) {
    var slug = project.main().jarslug();
    var builder = new StringBuilder(module);
    if (!slug.isEmpty()) builder.append('@').append(slug);
    return builder.append(".jar").toString();
  }

  /** @return the javadoc call generating the API documentation for all main modules */
  public ToolCall computeMainDocumentationJavadocCall() {
    var main = project.main();
    var api = main.documentation("api");
    return Command.builder("javadoc")
        .with("--module", String.join(",", main.modules()))
        .with("--module-source-path", String.join(File.pathSeparator, main.moduleSourcePaths()))
        .with("--module-path", String.join(File.pathSeparator, main.modulePaths()))
        .withEach(main.tweaks().getOrDefault("javadoc", List.of()))
        .with("-d", api)
        .build();
  }

  /** @return the jar call generating the API documentation archive */
  public ToolCall computeMainDocumentationJarCall() {
    var main = project.main();
    var api = main.documentation("api");
    var file = project.name() + "-api-" + project.version() + ".zip";
    return Command.builder("jar")
        .with("--create")
        .with("--file", api.getParent().resolve(file))
        .with("--no-manifest")
        .with("-C", api, ".")
        .build();
  }

  /** @return the jllink call */
  public ToolCall computeMainJLinkCall() {
    var main = project.main();
    var test = project.test();
    return Command.builder("jlink")
        .with("--add-modules", String.join(",", main.modules()))
        .with("--module-path", String.join(File.pathSeparator, test.modulePaths()))
        .withEach(main.tweaks().getOrDefault("jlink", List.of()))
        .with("--output", main.workspace("image"))
        .build();
  }

  /** @return the jpackage call */
  public ToolCall computeMainJPackageCall() {
    return Command.builder("jpackage").build();
  }

  /** @return the {@code javac} call to compile all modules of the test space. */
  public ToolCall computeTestJavacCall() {
    var test = project.test();
    return Command.builder("javac")
        .with("--module", String.join(",", test.modules()))
        .with("--module-source-path", String.join(File.pathSeparator, test.moduleSourcePaths()))
        .with("--module-path", String.join(File.pathSeparator, test.modulePaths()))
        .withEach(test.tweaks().getOrDefault("javac", List.of()))
        .with("-d", test.classes())
        .build();
  }

  /**
   * @param module the name of the module to create an archive for
   * @return the {@code jar} call to archive all assets for the given module
   */
  public ToolCall computeTestJarCall(String module) {
    var test = project.test();
    var archive = module + "@" + project.version() + "+test.jar";
    return Command.builder("jar")
        .with("--create")
        .with("--file", test.workspace("modules-test", archive))
        .withEach(test.tweaks().getOrDefault("jar", List.of()))
        .withEach(test.tweaks().getOrDefault("jar(" + module + ')', List.of()))
        .with("-C", test.classes().resolve(module), ".")
        .build();
  }

  /**
   * @param module the name of the module to scan for tests
   * @return the {@code junit} call to launch the JUnit Platform for
   */
  public ToolCall computeTestJUnitCall(String module) {
    var test = project.test();
    return Command.builder("junit")
        .with("--select-module", module)
        .with("--reports-dir", test.workspace("reports", "junit-test", module))
        .withEach(test.tweaks().getOrDefault("junit", List.of()))
        .withEach(test.tweaks().getOrDefault("junit(" + module + ')', List.of()))
        .build();
  }
}
