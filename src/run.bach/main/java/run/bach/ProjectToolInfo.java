package run.bach;

import run.bach.tool.BuildTool;
import run.bach.tool.CacheTool;
import run.bach.tool.CleanTool;
import run.bach.tool.CompileClassesTool;
import run.bach.tool.CompileModulesTool;
import run.bach.tool.CompileTool;
import run.bach.tool.LaunchTool;
import run.bach.tool.TestTool;
import run.duke.Tool;
import run.duke.ToolInfo;

public enum ProjectToolInfo implements ToolInfo {
  BUILD(BuildTool.NAME, BuildTool::new),
  CACHE(CacheTool.NAME, CacheTool::new),
  CLEAN(CleanTool.NAME, CleanTool::new),
  COMPILE(CompileTool.NAME, CompileTool::new),
  COMPILE_CLASSES(CompileClassesTool.NAME, CompileClassesTool::new),
  COMPILE_MODULES(CompileModulesTool.NAME, CompileModulesTool::new),
  LAUNCH(LaunchTool.NAME, LaunchTool::new),
  TEST(TestTool.NAME, TestTool::new);

  final String namespace;
  final String nickname;
  final String identifier;
  final ProjectToolFactory factory;

  ProjectToolInfo(String nickname, ProjectToolFactory factory) {
    this.namespace = Tool.namespace(ProjectToolInfo.class);
    this.nickname = nickname;
    this.identifier = Tool.identifier(namespace, nickname);
    this.factory = factory;
  }

  @Override
  public String namespace() {
    return namespace;
  }

  @Override
  public String nickname() {
    return nickname;
  }

  @Override
  public String identifier() {
    return identifier;
  }

  public Tool tool(Project project, ProjectToolRunner runner) {
    return new Tool(factory.createProjectTool(project, runner));
  }
}
