package run.bach;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import run.bach.tool.BuildTool;
import run.bach.tool.CacheTool;
import run.bach.tool.CleanTool;
import run.bach.tool.CompileClassesTool;
import run.bach.tool.CompileModulesTool;
import run.bach.tool.CompileTool;
import run.bach.tool.LaunchTool;
import run.bach.tool.TestTool;
import run.duke.Tool;
import run.duke.ToolFinder;
import run.duke.ToolRunner;

public final class ProjectToolFinder implements ToolFinder {
  private static final String NAMESPACE = "run.bach/";
  private Project project;

  public ProjectToolFinder() {}

  @Override
  public String description() {
    return "Bach's Project Tools";
  }

  @Override
  public List<String> identifiers() {
    return List.of(
        NAMESPACE + "build",
        NAMESPACE + "cache",
        NAMESPACE + "clean",
        NAMESPACE + "compile",
        NAMESPACE + "compile-classes",
        NAMESPACE + "compile-modules",
        NAMESPACE + "launch",
        NAMESPACE + "test");
  }

  @Override
  public Optional<Tool> find(String name, ToolRunner runner) {
    var workbench = (Workbench) runner;
    if (project == null) {
      var layer = workbench.toolbox().layer();
      var factory = ServiceLoader.load(layer, ProjectFactory.class).findFirst().orElseThrow();
      project = factory.createProject(workbench);
    }
    var tool =
        switch (name) {
          case "build", NAMESPACE + "build" -> new BuildTool(project, workbench);
          case "cache", NAMESPACE + "cache" -> new CacheTool(project, workbench);
          case "clean", NAMESPACE + "clean" -> new CleanTool(project, workbench);
          case "compile", NAMESPACE + "compile" -> new CompileTool(project, workbench);
          case "compile-classes", NAMESPACE + "compile-classes" //
          -> new CompileClassesTool(project, workbench);
          case "compile-modules", NAMESPACE + "compile-modules" //
          -> new CompileModulesTool(project, workbench);
          case "launch", NAMESPACE + "launch" -> new LaunchTool(project, workbench);
          case "test", NAMESPACE + "test" -> new TestTool(project, workbench);
          default -> null;
        };
    return Optional.ofNullable(tool).map(Tool::new);
  }
}
