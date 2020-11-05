package com.github.sormuras.bach;

import com.github.sormuras.bach.module.ModuleDirectory;
import com.github.sormuras.bach.tool.Command;
import com.github.sormuras.bach.tool.ToolCall;
import com.github.sormuras.bach.tool.ToolRunner;
import java.io.File;
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
      runner.run(computeMainJavacCall());
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
        .with("--module-source-path", String.join(File.pathSeparator, main.moduleSourcePaths()))
        .withEach(main.tweaks().getOrDefault("javac", List.of()))
        .with("-d", project.base().classes(main.name(), main.release()))
        .build();
  }
}
