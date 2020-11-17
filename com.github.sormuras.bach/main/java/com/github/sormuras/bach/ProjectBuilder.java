package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.Paths;
import com.github.sormuras.bach.module.ModuleDirectory;
import com.github.sormuras.bach.module.ModuleSearcher;
import com.github.sormuras.bach.project.MainSpace;
import com.github.sormuras.bach.project.TestSpace;
import com.github.sormuras.bach.tool.Command;
import com.github.sormuras.bach.tool.ToolCall;
import com.github.sormuras.bach.tool.ToolRunner;
import java.io.File;
import java.lang.module.ModuleFinder;
import java.util.List;
import java.util.Locale;

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

  /**
   * Print a formatted message.
   *
   * @param format the message to print
   * @param args the arguments
   */
  public void info(String format, Object... args) {
    if (args.length == 0) bach.printStream().println(format);
    else bach.printStream().format(Locale.ENGLISH, format + "%n", args);
  }

  /** Builds a modular Java project. */
  public void build() {
    info("Build project %s %s", project.name(), project.version());

    if (project.findAllModuleNames().count() == 0) throw new RuntimeException("No module found!");

    loadRequiredAndMissingModules();

    build(project.main());
    build(project.test());
  }

  /** Load required and missing modules in a best-effort manner. */
  public void loadRequiredAndMissingModules() {
    var searcher = ModuleSearcher.ofBestEffort(bach);
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
    runner.run(computeMainJavacCall()).checkSuccessful();
    Paths.createDirectories(main.workspace("modules"));
    for (var module : project.main().modules()) {
      runner.run(computeMainJarCall(module)).checkSuccessful();
    }

    if (isGenerateApiDocumentation()) {
      info("Generate API documentation");
      runner.run(computeMainDocumentationJavadocCall()).checkSuccessful();
      runner.run(computeMainDocumentationJarCall()).checkSuccessful();
    }

    if (isGenerateCustomRuntimeImage()) {
      info("Generate custom runtime image");
      Paths.deleteDirectories(main.workspace("image"));
      runner.run(computeMainJLinkCall()).checkSuccessful();
    }

    if (isGenerateApplicationPackage()) {
      info("Generate self-contained Java application");
      runner.run(computeMainJPackageCall()).checkSuccessful();
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
    runner.run(computeTestJavacCall()).checkSuccessful();
    Paths.createDirectories(test.workspace("modules-test"));
    for (var module : project.test().modules()) {
      runner.run(computeTestJarCall(module)).checkSuccessful();
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
        runner.run(computeTestJUnitCall(module), finder, module).checkSuccessful();
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
    var main = project.main();
    return Command.builder("javac")
        .with("--release", main.release())
        .with("--module", String.join(",", main.modules()))
        .with("--module-version", project.version())
        .with("--module-source-path", String.join(File.pathSeparator, main.moduleSourcePaths()))
        .with("--module-path", String.join(File.pathSeparator, main.modulePaths()))
        .withEach(main.tweaks().getOrDefault("javac", List.of()))
        .with("-d", main.classes())
        .build();
  }

  /**
   * @param module the name of module to create an archive for
   * @return the {@code jar} call to archive all assets for the given module
   */
  public ToolCall computeMainJarCall(String module) {
    var main = project.main();
    var archive = computeMainJarFileName(module);
    return Command.builder("jar")
        .with("--create")
        .with("--file", main.workspace("modules", archive))
        .withEach(main.tweaks().getOrDefault("jar", List.of()))
        .withEach(main.tweaks().getOrDefault("jar(" + module + ')', List.of()))
        .with("-C", main.classes().resolve(module), ".")
        .build();
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
