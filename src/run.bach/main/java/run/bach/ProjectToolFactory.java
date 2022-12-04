package run.bach;

@FunctionalInterface
public interface ProjectToolFactory {
  ProjectTool createProjectTool(Project project, ProjectToolRunner runner);
}
