package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.Paths;
import com.github.sormuras.bach.module.ModuleDirectory;
import com.github.sormuras.bach.tool.Command;
import com.github.sormuras.bach.tool.ToolCall;
import com.github.sormuras.bach.tool.ToolRunner;
import java.util.List;

/** Builds a modular Java project. */
public class ProjectBuilder {

  private final Bach bach;
  private final Project project;

  /**
   * Initialize this project builder with the given components.
   *
   * @param bach the {@code Bach} instance
   * @param project the project to build
   */
  public ProjectBuilder(Bach bach, Project project) {
    this.bach = bach;
    this.project = project;
  }

  /** Builds a modular Java project. */
  public void build() {
    bach.printStream().println("Work on " + project);
    // load missing modules

    var runner = new ToolRunner(ModuleDirectory.of(project.base().libraries()).finder());
    // main: javac + jar
    // main: javadoc
    // main: jlink
    // main: jpackage
    if (project.main().isPresent()) {
      runner.run(computeMainJavacCall()).checkSuccessful();
      Paths.createDirectories(project.base().workspace("modules"));
      for (var module : project.main().modules()) {
        runner.run(computeMainJarCall(module)).checkSuccessful();
      }
    }

    // test: javac + jar
    // test: junit

    // test-preview: javac + jar
    // test-preview: junit
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
        .with("-C", main.classes(project), ".")
        // .with(sources, (call, source) -> call.with("-C", source, "."))
        // .with(resources, (call, resource) -> call.with("-C", resource, "."))
        .build();
  }
}
