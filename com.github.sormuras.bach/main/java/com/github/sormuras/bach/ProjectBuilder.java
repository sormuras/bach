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
   */
  public void build(MainSpace main) {
    if (main.modules().isEmpty()) return;
    runner.run(computeMainJavacCall()).checkSuccessful();
    Paths.createDirectories(project.base().workspace("modules"));
    for (var module : project.main().modules()) {
      runner.run(computeMainJarCall(module)).checkSuccessful();
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
    var archive = module + "@" + project.version() + ".jar";
    return Command.builder("jar")
        .with("--create")
        .with("--file", project.base().workspace("modules", archive))
        // .with(unit.descriptor().mainClass(), Jar::withMainClass)
        .with("-C", main.classes(project).resolve(module), ".")
        // .with(sources, (call, source) -> call.with("-C", source, "."))
        // .with(resources, (call, resource) -> call.with("-C", resource, "."))
        .build();
  }
}
