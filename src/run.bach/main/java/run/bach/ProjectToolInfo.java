package run.bach;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import run.bach.tool.BuildTool;
import run.bach.tool.CacheTool;
import run.bach.tool.CleanTool;
import run.bach.tool.CompileClassesTool;
import run.bach.tool.CompileModulesTool;
import run.bach.tool.CompileTool;
import run.bach.tool.LaunchTool;
import run.bach.tool.TestTool;

public enum ProjectToolInfo {
  BUILD(BuildTool.NAME, "run.bach" + '/' + BuildTool.NAME),
  CACHE(CacheTool.NAME, "run.bach" + '/' + CacheTool.NAME),
  CLEAN(CleanTool.NAME, "run.bach" + '/' + CleanTool.NAME),
  COMPILE(CompileTool.NAME, "run.bach" + '/' + CompileTool.NAME),
  COMPILE_CLASSES(CompileClassesTool.NAME, "run.bach" + '/' + CompileClassesTool.NAME),
  COMPILE_MODULES(CompileModulesTool.NAME, "run.bach" + '/' + CompileModulesTool.NAME),
  LAUNCH(LaunchTool.NAME, "run.bach" + '/' + LaunchTool.NAME),
  TEST(TestTool.NAME, "run.bach" + '/' + TestTool.NAME);

  static Optional<ProjectToolInfo> find(String string) {
    for (var info : ProjectToolInfo.values()) {
      if (string.equals(info.name) || string.equals(info.identifier)) return Optional.of(info);
    }
    return Optional.empty();
  }

  static List<String> identifiers() {
    return Stream.of(values()).map(ProjectToolInfo::identifier).toList();
  }

  final String name;
  final String identifier;

  ProjectToolInfo(String name, String identifier) {
    this.name = name;
    this.identifier = identifier;
  }

  String identifier() {
    return identifier;
  }
}
