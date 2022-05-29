package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.ToolCallTweak;
import com.github.sormuras.bach.ToolOperator;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;

public class CompileClasses implements ToolOperator {

  static final String NAME = "compile-classes";

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    var project = bach.project();
    var paths = bach.configuration().paths();
    var spaces = project.spaces();
    var space = spaces.space(args[0]); // TODO Better argument handling
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

    javac = javac.with(space.tweak(ToolCallTweak.WORKFLOW_COMPILE_CLASSES_JAVAC));

    bach.run(javac);

    return 0;
  }
}
