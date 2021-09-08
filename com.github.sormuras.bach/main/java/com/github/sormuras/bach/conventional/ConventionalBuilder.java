package com.github.sormuras.bach.conventional;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.Grabber;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.ToolFinder;
import com.github.sormuras.bach.ToolRun;
import com.github.sormuras.bach.command.Composer;
import com.github.sormuras.bach.command.DefaultCommand;
import com.github.sormuras.bach.command.JarCommand;
import com.github.sormuras.bach.command.JavacCommand;
import com.github.sormuras.bach.command.ModulePathsOption;
import com.github.sormuras.bach.internal.ModuleFinderSupport;
import java.io.File;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class ConventionalBuilder {

  protected final Bach bach;
  protected final ConventionalSpace space;

  public ConventionalBuilder(Bach bach, ConventionalSpace space) {
    this.bach = bach;
    this.space = space;
  }

  public ConventionalBuilder grab(Grabber grabber, String... externalModules) {
    grabber.grabExternalModules(externalModules);
    grabber.grabMissingExternalModules();
    return this;
  }

  public ConventionalBuilder compile() {
    return compile(Composer.identity(), Composer.identity());
  }

  public ConventionalBuilder compile(
      Composer<JavacCommand> javacComposer, Composer<JarCommand> jarComposer) {
    var classesDirectory = outputDirectoryForClasses();
    var javac =
        new JavacCommand()
            .release(space.release().orElse(null))
            .modules(space.toModuleNames())
            .option(space.moduleSourcePaths())
            .option(computeModulePathOption())
            .outputDirectoryForClasses(classesDirectory);
    bach.run(javacComposer.apply(javac));

    var modulesDirectory = outputDirectoryForModules();
    bach.run(Command.of("directories", "clean", modulesDirectory));
    var jars = new ArrayList<JarCommand>();
    for (var module : space.modules()) {
      var jarCommand =
          new JarCommand()
              .mode("--create")
              .file(modulesDirectory.resolve(module.name() + ".jar"))
              .main(module.main().orElse(null))
              .filesAdd(classesDirectory.resolve(module.name()));
      jars.add(jarComposer.apply(jarCommand));
    }
    jars.parallelStream().forEach(bach::run);

    return this;
  }

  public void document(Composer<DefaultCommand> javadocComposer) {
    var api = bach.path().workspace(space.name().orElse(""), "documentation", "api");
    var javadoc =
        Command.of("javadoc")
            .add("--module", String.join(",", space.toModuleNames()))
            .add(
                "--module-source-path",
                String.join(File.pathSeparator, space.moduleSourcePaths().values()))
            .add("-d", api);
    if (!space.modulePaths().isPresent()) {
      javadoc = javadoc.add("--module-path", space.modulePaths().join(File.pathSeparator));
    }

    bach.run(javadocComposer.apply(javadoc));

    var jar =
        Command.jar()
            .mode("--create")
            .file(api.getParent().resolve("api.zip"))
            .add("--no-manifest")
            .filesAdd(api);
    bach.run(jar);
  }

  public void link(Composer<DefaultCommand> jlinkComposer) {
    var image = bach.path().workspace(space.name().orElse(""), "image");
    var jlink =
        Command.of("jlink")
            .add("--output", image)
            .add("--add-modules", String.join(",", space.toModuleNames()))
            .add(
                "--module-path",
                String.join(
                    File.pathSeparator,
                    outputDirectoryForModules().toString(),
                    bach.path().externalModules().toString()));

    bach.run("directories", run -> run.add("delete").add(image));
    bach.run(jlinkComposer.apply(jlink));
  }

  public Path outputDirectoryForClasses() {
    return bach.path().workspace(space.name().orElse(""), "classes");
  }

  public Path outputDirectoryForModules() {
    return bach.path().workspace(space.name().orElse(""), "modules");
  }

  public Path outputDirectoryForReports() {
    return bach.path().workspace(space.name().orElse(""), "reports");
  }

  private ModulePathsOption computeModulePathOption() {
    var option = space.modulePaths();
    var externalModules = bach.path().externalModules();
    if (option.values().contains(externalModules)) return option;
    if (!Files.isDirectory(externalModules)) return option;
    return option.add(externalModules);
  }

  private ModuleFinder computeRuntimeModuleFinder() {
    var paths = new ArrayList<Path>();
    paths.add(outputDirectoryForModules());
    paths.addAll(computeModulePathOption().values());
    return ModuleFinder.of(paths.toArray(Path[]::new));
  }

  public ConventionalSpace newDependentConventionalSpace(String name) {
    return ConventionalSpace.of(name).modulePaths(outputDirectoryForModules());
  }

  public void runAllTests() {
    bach.logCaption("Run all tests in %s space".formatted(space.name().orElse("this")));
    var moduleFinder = computeRuntimeModuleFinder();
    for (var module : space.modules()) {
      var name = module.name();
      if (ModuleFinderSupport.findMainClass(moduleFinder, name).isPresent()) {
        runModule(moduleFinder, name, Composer.identity(), bach.printer()::print);
      }
      var toolFinder = ToolFinder.of(moduleFinder, true, name);
      runTool(toolFinder, "test", Composer.identity(), bach.printer()::print);
      if (toolFinder.find("junit").isPresent()) runJUnit(toolFinder, name);
    }
  }

  public void runModule(String module, Composer<DefaultCommand> composer) {
    runModule(computeRuntimeModuleFinder(), module, composer, bach.printer()::print);
  }

  public void runModule(
      ModuleFinder finder,
      String module,
      Composer<DefaultCommand> composer,
      ToolRun.Visitor visitor) {
    var command = composer.apply(Command.of(module));
    var call = ToolCall.module(finder, command);
    var run = bach.run(call);
    visitor.accept(run);
  }

  public void runTool(String tool, Composer<DefaultCommand> composer) {
    var toolFinder = ToolFinder.of(computeRuntimeModuleFinder(), true);
    runTool(toolFinder, tool, composer, bach.printer()::print);
  }

  public void runTool(
      ToolFinder finder, String tool, Composer<DefaultCommand> composer, ToolRun.Visitor visitor) {
    for (var provider : finder.list(tool)) {
      var singleton = ToolFinder.of(provider);
      var command = composer.apply(Command.of(tool));
      var call = ToolCall.of(singleton, command);
      var run = bach.run(call);
      visitor.accept(run);
    }
  }

  public void runJUnit(String module) {
    var toolFinder = ToolFinder.of(computeRuntimeModuleFinder(), true, module);
    runJUnit(toolFinder, module);
  }

  public void runJUnit(ToolFinder finder, String module) {
    runJUnit(finder, module, Composer.identity(), ToolRun.Visitor.noop());
  }

  public void runJUnit(
      ToolFinder finder,
      String module,
      Composer<DefaultCommand> composer,
      ToolRun.Visitor visitor) {
    var reports = outputDirectoryForReports().resolve("junit/" + module);
    var junit = Command.of("junit").add("--select-module", module).add("--reports-dir", reports);
    var call = ToolCall.of(finder, composer.apply(junit));
    var run = bach.run(call);
    visitor.accept(run);
  }
}
