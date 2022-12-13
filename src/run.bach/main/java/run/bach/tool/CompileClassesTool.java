package run.bach.tool;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import run.bach.Project;
import run.bach.ProjectTool;
import run.bach.ProjectToolRunner;
import run.duke.ToolCall;

public class CompileClassesTool extends ProjectTool {
  public static ToolCall compile(Project.Space space) {
    return ToolCall.of("compile-classes", space.name());
  }

  public CompileClassesTool(ProjectToolRunner runner) {
    super("compile-classes", runner);
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var space = project().spaces().space(args[0]);
    var javac = createJavacCall();
    javac = javacWithRelease(javac, space);
    javac = javacWithModules(javac, space);
    javac = javacWithModuleSourcePaths(javac, space);
    javac = javacWithModulePaths(javac, space);
    javac = javacWithModulePatches(javac, space);
    javac = javacWithDestinationDirectory(javac, space);
    run(javac);
    return 0;
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

  protected ToolCall javacWithModulePaths(ToolCall javac, Project.Space space) {
    var modulePath = space.toModulePath(folders());
    if (modulePath.isPresent()) {
      javac = javac.with("--module-path", modulePath.get());
      javac = javac.with("--processor-module-path", modulePath.get());
    }
    return javac;
  }

  protected ToolCall javacWithModulePatches(ToolCall javac, Project.Space space) {
    for (var declaration : space.modules().list()) {
      var module = declaration.name();
      var patches = new ArrayList<String>();
      for (var requires : space.requires()) {
        if (project().spaces().space(requires).modules().find(module).isEmpty()) {
          continue;
        }
        patches.add(folders().out(requires, "modules", module + ".jar").toString());
      }
      if (patches.isEmpty()) continue;
      var patch = String.join(File.pathSeparator, patches);
      javac = javac.with("--patch-module", module + "=" + patch);
    }
    return javac;
  }

  protected ToolCall javacWithDestinationDirectory(ToolCall javac, Project.Space space) {
    var feature = space.targets().orElse(Runtime.version().feature());
    var classes = folders().out(space.name(), "classes").resolve("java-" + feature);
    return javac.with("-d", classes);
  }
}
