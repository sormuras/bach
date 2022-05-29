package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.ToolCallTweak;
import com.github.sormuras.bach.ToolOperator;
import com.github.sormuras.bach.internal.ModuleSourcePathSupport;
import com.github.sormuras.bach.project.DeclaredModule;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.stream.Collectors;

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
    var space = spaces.space(args[0]);
    var declarations = space.modules().list();
    var classes = paths.out(space.name(), "classes");

    var javac = ToolCall.of("javac");

    var release0 = space.targets();
    if (release0.isPresent()) {
      javac = javac.with("--release", release0.get());
    }

    javac = javac.with("--module", space.modules().names(","));

    var map =
        declarations.stream()
            .collect(Collectors.toMap(DeclaredModule::name, DeclaredModule::baseSourcePaths));
    for (var moduleSourcePath : ModuleSourcePathSupport.compute(map, false)) {
      javac = javac.with("--module-source-path", moduleSourcePath);
    }

    var modulePath = space.toModulePath(paths);
    if (modulePath.isPresent()) {
      javac = javac.with("--module-path", modulePath.get());
      javac = javac.with("--processor-module-path", modulePath.get());
    }

    // --patch-module
    for (var declaration : declarations) {
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
