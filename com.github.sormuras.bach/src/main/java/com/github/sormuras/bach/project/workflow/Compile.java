package com.github.sormuras.bach.project.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.ToolOperator;
import com.github.sormuras.bach.internal.PathSupport;
import com.github.sormuras.bach.project.DeclaredModule;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Compile implements ToolOperator {
  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    var project = bach.project();
    var paths = bach.configuration().paths();
    var spaces = project.spaces();

    for (var space : spaces.list()) {
      var declarations = space.modules().list();
      if (declarations.isEmpty()) {
        out.println("No %s modules declared.".formatted(space.name()));
        continue;
      }
      out.println(
          "Compile and package %d %s module%s..."
              .formatted(declarations.size(), space.name(), declarations.size() == 1 ? "" : "s"));

      var classes = paths.out(space.name(), "classes");
      var modules = paths.out(space.name(), "modules");

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

      var externalModules = Stream.of(paths.root(".bach", "external-modules"));
      var requiredModules =
          space.requires().stream().map(required -> paths.out(required, "modules"));
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

      try {
        Files.createDirectories(modules);
      } catch (Exception exception) {
        throw new RuntimeException("Create directories failed: " + modules);
      }

      var javacCommands = new ArrayList<ToolCall>();
      var jarCommands = new ArrayList<ToolCall>();
      var jarFiles = new ArrayList<Path>();
      for (var module : declarations) {
        var name = module.name();
        var file = modules.resolve(name + ".jar");

        jarFiles.add(file);

        var jar = ToolCall.of("jar").with("--create").with("--file", file);

        jar = jar.with("--module-version", project.version().value());

        if (Runtime.version().feature() >= 19) {
          var date = project.version().date();
          jar = jar.with("--date", date.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }

        var mainProgram = name.replace('.', '/') + "/Main.java";
        var mainJava = module.toModuleSourcePaths().get(0).resolve(mainProgram);
        if (Files.exists(mainJava)) {
          jar = jar.with("--main-class", name + ".Main");
        }

        jar = jar.with("-C", classes0.resolve(name), ".");

        // include classes of patched module
        for (var requires : space.requires()) {
          var required = spaces.space(requires);
          if (required.modules().find(name).isPresent()) {
            var javaR = "java-" + required.targets().orElse(Runtime.version().feature());
            jar = jar.with("-C", paths.out(requires, "classes", javaR, name), ".");
          }
        }

        for (var folder : module.folders().list().stream().skip(1).toList()) {
          if ("java-module".equals(folder.path().getFileName().toString())) continue;
          var release = folder.release();
          var classesR = classes.resolve("java-" + release).resolve(name);
          javacCommands.add(
              ToolCall.of("javac")
                  .with("--release", release)
                  .with("-d", classesR)
                  .withFindFiles(folder.path(), "**.java"));
          jar = jar.with("--release", release).with("-C", classesR, ".");
        }

        jarCommands.add(jar);
      }
      javacCommands.stream().parallel().forEach(bach::run);
      jarCommands.stream().parallel().forEach(bach::run);

      jarFiles.forEach(
          file -> {
            if (Files.notExists(file)) {
              out.println("JAR file not found: " + file);
              return;
            }
            var size = PathSupport.computeChecksum(file, "SIZE");
            var hash = PathSupport.computeChecksum(file, "SHA-256");
            out.println("%s %11s %s".formatted(hash, size, file));
          });
    }
    return 0;
  }
}
