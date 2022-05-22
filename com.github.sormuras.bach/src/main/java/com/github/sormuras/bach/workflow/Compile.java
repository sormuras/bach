package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.ToolOperator;
import com.github.sormuras.bach.internal.ModuleSourcePathSupport;
import com.github.sormuras.bach.project.DeclaredModule;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Compile implements ToolOperator {
  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    var project = bach.project();
    var paths = bach.configuration().paths();
    var spaces = project.spaces();
    var space = spaces.space(args[0]);
    var declarations = space.modules().list();

    out.println(
        "Compile %d %s module%s..."
            .formatted(declarations.size(), space.name(), declarations.size() == 1 ? "" : "s"));

    var classes = paths.out(space.name(), "classes");

    var javac = ToolCall.of("javac");

    var release0 = space.targets();
    if (release0.isPresent()) {
      javac = javac.with("--release", release0.get());
    }

    javac =
        javac.with(
            "--module",
            declarations.stream().map(DeclaredModule::name).collect(Collectors.joining(",")));
    var map =
        declarations.stream()
            .collect(Collectors.toMap(DeclaredModule::name, DeclaredModule::toModuleSourcePaths));
    for (var moduleSourcePath : ModuleSourcePathSupport.compute(map, false)) {
      javac = javac.with("--module-source-path", moduleSourcePath);
    }

    var externalModules = Stream.of(paths.externalModules());
    var requiredModules = space.requires().stream().map(required -> paths.out(required, "modules"));
    var modulePath =
        Stream.concat(requiredModules, externalModules)
            .filter(Files::isDirectory)
            .map(Path::toString)
            .toList();
    if (!modulePath.isEmpty()) {
      var path = String.join(File.pathSeparator, modulePath);
      javac = javac.with("--module-path", path);
      javac = javac.with("--processor-module-path", path);
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

    for (var additionalCompileJavacArgument : space.additionalCompileJavacArguments()) {
      javac = javac.with(additionalCompileJavacArgument);
    }

    bach.run(javac);

    return 0;
  }
}
