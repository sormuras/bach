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
  private /*lazy*/ Project project;

  public ProjectToolFinder() {}

  @Override
  public String description() {
    return "Bach's Project Tools";
  }

  @Override
  public List<String> identifiers() {
    return ProjectToolInfo.identifiers();
  }

  @Override
  public Optional<Tool> find(String string, ToolRunner runner) {
    return ProjectToolInfo.find(string)
        .map(info -> toProjectTool(info, (Workbench) runner))
        .map(Tool::new);
  }

  ProjectTool toProjectTool(ProjectToolInfo info, Workbench workbench) {
    if (project == null) {
      var layer = workbench.toolbox().layer();
      var factory = ServiceLoader.load(layer, ProjectFactory.class).findFirst().orElseThrow();
      project = factory.createProject(workbench);
    }
    return switch (info) {
      case BUILD -> new BuildTool(project, workbench);
      case CACHE -> new CacheTool(project, workbench);
      case CLEAN -> new CleanTool(project, workbench);
      case COMPILE -> new CompileTool(project, workbench);
      case COMPILE_CLASSES -> new CompileClassesTool(project, workbench);
      case COMPILE_MODULES -> new CompileModulesTool(project, workbench);
      case LAUNCH -> new LaunchTool(project, workbench);
      case TEST -> new TestTool(project, workbench);
    };
  }
}
