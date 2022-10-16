package run.bach.project.workflow;

import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import run.bach.Bach;
import run.bach.ToolCall;
import run.bach.ToolOperator;

public class CompileModules implements ToolOperator {

  static final String NAME = "compile-modules";

  public CompileModules() {}

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
      var mainJava =
          module.base().sources().stream()
              .map(dir -> dir.resolve(mainProgram))
              .filter(Files::isRegularFile)
              .findFirst();
      if (mainJava.isPresent()) {
        jar = jar.with("--main-class", name + ".Main");
      }

      // include base classes (from compile-classes) and resources
      if (Files.isDirectory(classes0.resolve(name))) {
        jar = jar.with("-C", classes0.resolve(name), ".");
      }
      for (var resources : module.base().resources()) {
        jar = jar.with("-C", resources, ".");
      }

      // include classes of patched module
      for (var requires : space.requires()) {
        var required = spaces.space(requires);
        if (required.modules().find(name).isPresent()) {
          var javaR = "java-" + required.targets().orElse(Runtime.version().feature());
          jar = jar.with("-C", paths.out(requires, "classes", javaR, name), ".");
        }
      }

      // compile and include targeted classes and resources
      for (var release : module.targeted().keySet().stream().sorted().toList()) {
        var folders = module.targeted().get(release);
        for (var sources : folders.sources()) {
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
                  .withFindFiles(sources, "**.java");
          javacCommands.add(javac);
          jar = jar.with("--release", release).with("-C", classesR, ".");
        }

        var needsReleaseArgument = folders.sources().isEmpty() && !folders.resources().isEmpty();
        if (needsReleaseArgument) jar = jar.with("--release", release);

        for (var resources : folders.resources()) {
          jar = jar.with("-C", resources, ".");
        }
      }

      jarCommands.add(jar);
    }
    javacCommands.stream().parallel().forEach(bach::run);
    jarCommands.stream().parallel().forEach(bach::run);

    bach.run("hash", modules.toString());
  }
}
