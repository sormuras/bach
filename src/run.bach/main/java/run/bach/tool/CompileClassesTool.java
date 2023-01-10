package run.bach.tool;

import java.io.File;
import java.util.ArrayList;
import run.bach.Folders;
import run.bach.Project;
import run.bach.ProjectOperator;
import run.bach.ProjectRunner;
import run.duke.ToolCall;
import run.duke.ToolLogger;

public class CompileClassesTool implements ProjectOperator {
  public static ToolCall compile(Project.Space space) {
    return ToolCall.of("compile-classes", space.name());
  }

  public CompileClassesTool() {}

  @Override
  public final String name() {
    return "compile-classes";
  }

  @Override
  public void run(ProjectRunner runner, ToolLogger logger, String... args) {
    var space = runner.project().spaces().space(args[0]);
    var javac = createJavacCall();
    javac = javacWithRelease(javac, space);
    javac = javacWithModules(javac, space);
    javac = javacWithModuleSourcePaths(javac, space);
    javac = javacWithModulePaths(javac, space, runner.folders());
    javac = javacWithModulePatches(javac, space, runner);
    javac = javacWithDestinationDirectory(javac, space, runner.folders());
    runner.run(javac);
  }

  protected ToolCall createJavacCall() {
    return ToolCall.of("javac");
  }

  protected ToolCall javacWithRelease(ToolCall javac, Project.Space space) {
    return space.targets().map(feature -> javac.with("--release", feature)).orElse(javac);
  }

  protected ToolCall javacWithModules(ToolCall javac, Project.Space space) {
    return javac.with("--module", space.modules().names(","));
  }

  protected ToolCall javacWithModuleSourcePaths(ToolCall javac, Project.Space space) {
    for (var moduleSourcePath : space.modules().toModuleSourcePaths()) {
      javac = javac.with("--module-source-path", moduleSourcePath);
    }
    return javac;
  }

  protected ToolCall javacWithModulePaths(ToolCall javac, Project.Space space, Folders folders) {
    var modulePath = space.toModulePath(folders);
    if (modulePath.isPresent()) {
      javac = javac.with("--module-path", modulePath.get());
      javac = javac.with("--processor-module-path", modulePath.get());
    }
    return javac;
  }

  protected ToolCall javacWithModulePatches(
      ToolCall javac, Project.Space space, ProjectRunner runner) {
    for (var declaration : space.modules().list()) {
      var module = declaration.name();
      var patches = new ArrayList<String>();
      for (var requires : space.requires()) {
        if (runner.project().spaces().space(requires).modules().find(module).isEmpty()) {
          continue;
        }
        patches.add(runner.folders().out(requires, "modules", module + ".jar").toString());
      }
      if (patches.isEmpty()) continue;
      var patch = String.join(File.pathSeparator, patches);
      javac = javac.with("--patch-module", module + "=" + patch);
    }
    return javac;
  }

  protected ToolCall javacWithDestinationDirectory(
      ToolCall javac, Project.Space space, Folders folders) {
    var feature = space.targets().orElse(Runtime.version().feature());
    var classes = folders.out(space.name(), "classes").resolve("java-" + feature);
    return javac.with("-d", classes);
  }
}
