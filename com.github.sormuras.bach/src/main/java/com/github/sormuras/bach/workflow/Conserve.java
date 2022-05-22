package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.ToolOperator;
import com.github.sormuras.bach.internal.PathSupport;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class Conserve implements ToolOperator {
  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    var project = bach.project();
    var paths = bach.configuration().paths();
    var spaces = project.spaces();
    var space = spaces.space(args[0]);
    var declarations = space.modules().list();

    out.println(
        "Conserve classes of %d %s module%s..."
            .formatted(declarations.size(), space.name(), declarations.size() == 1 ? "" : "s"));

    var classes = paths.out(space.name(), "classes");
    var modules = paths.out(space.name(), "modules");

    var release0 = space.targets();
    var classes0 = classes.resolve("java-" + release0.orElse(Runtime.version().feature()));

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

    return 0;
  }
}
