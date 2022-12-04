package run.bach;

import java.util.List;
import java.util.ServiceLoader;
import run.duke.EnumToolFinder;
import run.duke.Tool;

public final class ProjectToolFinder implements EnumToolFinder<ProjectToolInfo, ProjectToolRunner> {
  private final List<ProjectToolInfo> constants;
  private /*lazy*/ Project project;

  public ProjectToolFinder() {
    this.constants = List.of(ProjectToolInfo.values());
  }

  @Override
  public List<ProjectToolInfo> constants() {
    return constants;
  }

  @Override
  public String description() {
    return "Bach's Project Tools";
  }

  @Override
  public Tool tool(ProjectToolInfo info, ProjectToolRunner runner) {
    if (project == null) {
      var layer = runner.toolbox().layer();
      var factory = ServiceLoader.load(layer, ProjectFactory.class).findFirst().orElseThrow();
      project = factory.createProject(runner);
    }
    return info.tool(project, runner);
  }
}
