package run.bach.project;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import run.bach.Bach;
import run.bach.Project;
import run.bach.ToolCall;
import run.bach.ToolOperator;

public class CompileClassesTool implements ToolOperator {

  static final String NAME = "compile-classes";

  public CompileClassesTool() {}

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
    var space = bach.project().spaces().space(arguments.get(0)); // TODO Better argument handling
    var context = new OperationContext(bach, space);
    var javac = createJavacCall();
    javac = javacWithRelease(javac, context);
    javac = javacWithModules(javac, context);
    javac = javacWithModuleSourcePaths(javac, context);
    javac = javacWithModulePaths(javac, context);
    javac = javacWithModulePatches(javac, context);
    javac = javacWithDestinationDirectory(javac, context);
    bach.run(javac);
  }

  protected ToolCall createJavacCall() {
    return ToolCall.of("javac");
  }

  protected ToolCall javacWithRelease(ToolCall javac, OperationContext context) {
    return context.space().targets().map(feature -> javac.with("--release", feature)).orElse(javac);
  }

  protected ToolCall javacWithModules(ToolCall javac, OperationContext context) {
    return javac.with("--module", context.space().modules().names(","));
  }

  protected ToolCall javacWithModuleSourcePaths(ToolCall javac, OperationContext context) {
    for (var moduleSourcePath : context.space().modules().toModuleSourcePaths()) {
      javac = javac.with("--module-source-path", moduleSourcePath);
    }
    return javac;
  }

  protected ToolCall javacWithModulePaths(ToolCall javac, OperationContext context) {
    var modulePath = context.space().toModulePath(context.bach().paths());
    if (modulePath.isPresent()) {
      javac = javac.with("--module-path", modulePath.get());
      javac = javac.with("--processor-module-path", modulePath.get());
    }
    return javac;
  }

  protected ToolCall javacWithModulePatches(ToolCall javac, OperationContext context) {
    var paths = context.bach().paths();
    var spaces = context.bach().project().spaces();
    for (var declaration : context.space().modules().list()) {
      var module = declaration.name();
      var patches = new ArrayList<String>();
      for (var requires : context.space().requires()) {
        if (spaces.space(requires).modules().find(module).isEmpty()) {
          continue;
        }
        patches.add(paths.out(requires, "modules", module + ".jar").toString());
      }
      if (patches.isEmpty()) continue;
      var patch = String.join(File.pathSeparator, patches);
      javac = javac.with("--patch-module", module + "=" + patch);
    }
    return javac;
  }

  protected ToolCall javacWithDestinationDirectory(ToolCall javac, OperationContext context) {
    var paths = context.bach().paths();
    var space = context.space();
    var feature = space.targets().orElse(Runtime.version().feature());
    var classes = paths.out(space.name(), "classes").resolve("java-" + feature);
    return javac.with("-d", classes);
  }

  public record OperationContext(Bach bach, Project.Space space) {}
}
