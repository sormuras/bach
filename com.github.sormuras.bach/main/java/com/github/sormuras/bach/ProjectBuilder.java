package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.Paths;
import com.github.sormuras.bach.module.ModuleDirectory;
import com.github.sormuras.bach.project.MainSpace;
import com.github.sormuras.bach.tool.Command;
import com.github.sormuras.bach.tool.ToolCall;
import com.github.sormuras.bach.tool.ToolRunner;
import java.util.List;

/** Builds a modular Java project. */
public class ProjectBuilder {

  private final Bach bach;
  private final Project project;
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
    this.runner = new ToolRunner(ModuleDirectory.of(project.base().libraries()).finder());
  }

  /** Builds a modular Java project. */
  public void build() {
    bach.printStream().println("Build project " + project.name() + " " + project.version());
    // load missing modules

    build(project.main());
    // build(project.test())
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
    runner.run(computeMainJavacCall()).checkSuccessful();
    Paths.createDirectories(project.base().workspace("modules"));
    for (var module : project.main().modules()) {
      runner.run(computeMainJarCall(module)).checkSuccessful();
    }
    if (main.generateApiDocumentation()) {
      runner.run(computeMainDocumentationJavadocCall()).checkSuccessful();
      runner.run(computeMainDocumentationJarCall()).checkSuccessful();
    }
  }

  /** @return the {@code javac} call to compile all modules of the main space. */
  public ToolCall computeMainJavacCall() {
    var main = project.main();
    return Command.builder("javac")
        .with("--module", String.join(",", main.modules()))
        .with("--module-version", project.version())
        .with("--module-source-path", main.moduleSourcePath(project))
        .withEach(main.tweaks().getOrDefault("javac", List.of()))
        .with("-d", main.classes(project))
        .build();
  }

  /**
   * @param module the name of module to create an archive for
   * @return the {@code jar} call to jar all assets for the given module
   */
  public ToolCall computeMainJarCall(String module) {
    var main = project.main();
    var archive = computeMainJarFileName(module);
    return Command.builder("jar")
        .with("--create")
        .with("--file", project.base().workspace("modules", archive))
        // .with(unit.descriptor().mainClass(), Jar::withMainClass)
        .with("-C", main.classes(project).resolve(module), ".")
        // .with(sources, (call, source) -> call.with("-C", source, "."))
        // .with(resources, (call, resource) -> call.with("-C", resource, "."))
        .build();
  }

  /**
   * @param module the name of the module
   * @return the name of the JAR file for the given module
   */
  public String computeMainJarFileName(String module) {
    return module + "@" + project.version() + ".jar";
  }

  /** @return the javadoc call generating the API documentation for all main modules */
  public ToolCall computeMainDocumentationJavadocCall() {
    var main = project.main();
    var api = main.documentation(project, "api");
    return Command.builder("javadoc")
        .with("--module", String.join(",", main.modules()))
        .with("--module-source-path", main.moduleSourcePath(project))
        .withEach(main.tweaks().getOrDefault("javadoc", List.of()))
        .with("-d", api)
        .build();
  }

  /** @return the jar call generating the API documentation archive */
  public ToolCall computeMainDocumentationJarCall() {
    var main = project.main();
    var api = main.documentation(project, "api");
    var file = project.name() + "-api-" + project.version() + ".zip";
    return Command.builder("jar")
        .with("--create")
        .with("--file", api.getParent().resolve(file))
        .with("--no-manifest")
        .with("-C", api, ".")
        .build();
  }
}
