package run.bach;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Stream;
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
    return Stream.of(ProjectToolInfo.values()).map(ProjectToolInfo::identifier).toList();
  }

  @Override
  public Optional<Tool> find(String string, ToolRunner runner) {
    var casted = (ProjectToolRunner) runner;
    var values = ProjectToolInfo.values();
    for (var info : values) if (info.test(string)) return Optional.of(tool(info, casted));
    return Optional.empty();
  }

  Tool tool(ProjectToolInfo info, ProjectToolRunner runner) {
    if (project == null) {
      var layer = runner.toolbox().layer();
      var factory = ServiceLoader.load(layer, ProjectFactory.class).findFirst().orElseThrow();
      project = factory.createProject(runner);
    }
    return info.tool(project, runner);
  }
}
