package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.ToolOperator;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class CompileModules implements ToolOperator {

  static final String NAME = "compile-modules";

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

    for (var module : declarations) {
      var name = module.name();
      var file = modules.resolve(name + ".jar");

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

      for (var folder : module.folders().list()) {
        if ("java-module".equals(folder.path().getFileName().toString())) continue;
        var release = folder.release();
        if (folder.isSources()) {
          if (release == 0) continue;
          var classesR = classes.resolve("java-" + release).resolve(name);
          var javac = ToolCall.of("javac").with("--release", release);
          var modulePath = space.toModulePath(paths);
          if (modulePath.isPresent()) {
            javac = javac.with("--module-path", modulePath.get());
            javac = javac.with("--processor-module-path", modulePath.get());
          }
          javac =
              javac
                  .with("--class-path", classes0.resolve(name))
                  .with("-implicit:none")
                  .with("-d", classesR)
                  .withFindFiles(folder.path(), "**.java");
          javacCommands.add(javac);
          jar = jar.with("--release", release).with("-C", classesR, ".");
        }
        if (folder.isResources()) {
          if (release != 0) jar = jar.with("--release", release);
          jar = jar.with("-C", folder.path(), ".");
        }
      }

      jarCommands.add(jar);
    }
    javacCommands.stream().parallel().forEach(bach::run);
    jarCommands.stream().parallel().forEach(bach::run);

    bach.run("checksum", "--list-dir", modules);
    return 0;
  }
}
