package run.bach.project.workflow;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import run.bach.Bach;
import run.bach.ToolCall;
import run.bach.ToolOperator;

public class CompileClasses implements ToolOperator {

  static final String NAME = "compile-classes";

  public CompileClasses() {}

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
    var project = bach.project();
    var paths = bach.paths();
    var spaces = project.spaces();
    var space = spaces.space(arguments.get(0)); // TODO Better argument handling
    var classes = paths.out(space.name(), "classes");

    var javac = ToolCall.of("javac");

    var release0 = space.targets();
    if (release0.isPresent()) {
      javac = javac.with("--release", release0.get());
    }

    var modules = space.modules();
    javac = javac.with("--module", modules.names(","));

    for (var moduleSourcePath : modules.toModuleSourcePaths()) {
      javac = javac.with("--module-source-path", moduleSourcePath);
    }

    var modulePath = space.toModulePath(paths);
    if (modulePath.isPresent()) {
      javac = javac.with("--module-path", modulePath.get());
      javac = javac.with("--processor-module-path", modulePath.get());
    }

    // --patch-module
    for (var declaration : modules.list()) {
      var module = declaration.name();
      var patches = new ArrayList<String>();
      for (var requires : space.requires()) {
        if (spaces.space(requires).modules().find(module).isEmpty()) {
          continue;
        }
        patches.add(paths.out(requires, "modules", module + ".jar").toString());
      }
      if (patches.isEmpty()) continue;
      var patch = String.join(File.pathSeparator, patches);
      javac = javac.with("--patch-module", module + "=" + patch);
    }

    var classes0 = classes.resolve("java-" + release0.orElse(Runtime.version().feature()));
    javac = javac.with("-d", classes0);

    bach.run(javac);
  }
}
