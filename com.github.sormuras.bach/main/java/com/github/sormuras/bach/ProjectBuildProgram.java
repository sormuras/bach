package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.Modules;
import com.github.sormuras.bach.tool.Command;
import com.github.sormuras.bach.tool.ToolRunner;
import java.io.PrintStream;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ServiceLoader;

/** An extensible build program. */
public class ProjectBuildProgram {

  private final Bach bach;
  private final PrintStream out;

  /**
   * Initializes this build program with the given components.
   *
   * @param bach the calling Bach instance
   */
  public ProjectBuildProgram(Bach bach) {
    this.bach = bach;
    this.out = bach.printStream();
  }

  /** @return the configured instance of {@code Bach} */
  public Bach bach() {
    return bach;
  }

  /**
   * Builds a modular Java project.
   *
   * @param args the command line arguments
   */
  public void build(String... args) {
    out.println("Build started");
    if (Files.exists(Path.of(".bach", "build", "module-info.java")))
      buildWithBuildModule("build", args);
    else buildWithSystemDefaults();
  }

  private void buildWithBuildModule(String name, String... args) {
    var classes = Path.of(".bach/workspace/classes/." + name);
    var compile =
        Command.builder("javac")
            .with("--module", name)
            .with("--module-source-path", Path.of(".bach"))
            .with("--module-path", Path.of(".bach/cache"))
            .with("-d", classes)
            .build();
    var finder = ModuleFinder.of(classes, Path.of(".bach/cache"));
    var runner = new ToolRunner(finder);
    runner.run(compile).checkSuccessful();

    var layer = Modules.layer(finder, name);
    var builder = ServiceLoader.load(layer, ProjectBuilder.class).findFirst();
    if (builder.isPresent()) {
      var customProjectBuilder = builder.get();
      out.println("Delegating thread of control to: " + customProjectBuilder);
      customProjectBuilder.build(bach, args);
      return;
    }

    var module = layer.findModule(name).orElseThrow();
    var projectInfo = Optional.ofNullable(module.getAnnotation(ProjectInfo.class));
    projectInfo.ifPresentOrElse(this::buildWithProjectInfo, this::buildWithSystemDefaults);
  }

  private void buildWithProjectInfo(ProjectInfo info) {
    out.println("TODO #buildWithProjectInfo: " + info);
  }

  private void buildWithSystemDefaults() {
    out.println("TODO #buildWithSystemDefaults");
  }
}
