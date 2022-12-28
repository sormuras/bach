package run.bach.tool;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import run.bach.Bach;
import run.bach.Folders;
import run.bach.Project;
import run.duke.ToolCall;

public class CompileClassesTool implements Bach.Operator {
  public static ToolCall compile(Project.Space space) {
    return ToolCall.of("compile-classes", space.name());
  }

  public CompileClassesTool() {}

  @Override
  public final String name() {
    return "compile-classes";
  }

  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    var space = bach.project().spaces().space(args[0]);
    var javac = createJavacCall();
    javac = javacWithRelease(javac, space);
    javac = javacWithModules(javac, space);
    javac = javacWithModuleSourcePaths(javac, space);
    javac = javacWithModulePaths(javac, space, bach.folders());
    javac = javacWithModulePatches(javac, space, bach);
    javac = javacWithDestinationDirectory(javac, space, bach.folders());
    bach.run(javac);
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

  protected ToolCall javacWithModulePaths(ToolCall javac, Project.Space space, Folders folders) {
    var modulePath = space.toModulePath(folders);
    if (modulePath.isPresent()) {
      javac = javac.with("--module-path", modulePath.get());
      javac = javac.with("--processor-module-path", modulePath.get());
    }
    return javac;
  }

  protected ToolCall javacWithModulePatches(ToolCall javac, Project.Space space, Bach bach) {
    for (var declaration : space.modules().list()) {
      var module = declaration.name();
      var patches = new ArrayList<String>();
      for (var requires : space.requires()) {
        if (bach.project().spaces().space(requires).modules().find(module).isEmpty()) {
          continue;
        }
        patches.add(bach.folders().out(requires, "modules", module + ".jar").toString());
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
