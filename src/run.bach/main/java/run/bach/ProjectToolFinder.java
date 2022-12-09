package run.bach;

import java.util.ArrayList;
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
  private /*lazy*/ ToolFinder finder;

  public ProjectToolFinder() {}

  private synchronized void initialize(ProjectToolRunner runner) {
    if (project != null && finder != null) return;
    var layer = runner.toolbox().layer();
    var projectFactory = ServiceLoader.load(layer, Project.Factory.class).findFirst().orElseThrow();
    project = projectFactory.createProject(runner);
    var tools = new ArrayList<Tool>();
    for (var toolFactory : projectFactory.createProjectToolFactories()) {
      var projectTool = toolFactory.createProjectTool(project, runner);
      tools.add(new Tool(projectTool));
    }
    for (var toolFactory : ServiceLoader.load(layer, ProjectTool.Factory.class)) {
      var projectTool = toolFactory.createProjectTool(project, runner);
      tools.add(new Tool(projectTool));
    }
    /* add default project tools */ {
      tools.add(new Tool(new BuildTool(project, runner)));
      tools.add(new Tool(new CacheTool(project, runner)));
      tools.add(new Tool(new CleanTool(project, runner)));
      tools.add(new Tool(new CompileTool(project, runner)));
      tools.add(new Tool(new CompileClassesTool(project, runner)));
      tools.add(new Tool(new CompileModulesTool(project, runner)));
      tools.add(new Tool(new LaunchTool(project, runner)));
      tools.add(new Tool(new TestTool(project, runner)));
    }
    finder = ToolFinder.ofTools("Delegator", tools);
  }

  @Override
  public String description() {
    return "Bach's Project Tools";
  }

  @Override
  public List<String> identifiers(ToolRunner runner) {
    initialize((ProjectToolRunner) runner);
    return finder.identifiers(runner);
  }

  @Override
  public Optional<Tool> find(String string, ToolRunner runner) {
    initialize((ProjectToolRunner) runner);
    return finder.find(string, runner);
  }
}
