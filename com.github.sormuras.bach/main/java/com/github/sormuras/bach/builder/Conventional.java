package com.github.sormuras.bach.builder;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Grabber;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.ToolFinder;
import com.github.sormuras.bach.ToolRun;
import com.github.sormuras.bach.internal.ModuleDescriptorSupport;
import com.github.sormuras.bach.internal.ModuleFinderSupport;
import com.github.sormuras.bach.internal.PathSupport;
import java.io.File;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/** An API for building modular Java projects using conventional source file tree layouts. */
public sealed interface Conventional {

  /** Conventional module space descriptor. */
  record Space(
      Optional<String> name,
      List<ModuleUnit> moduleUnits,
      List<String> moduleSourcePaths,
      List<Path> modulePaths,
      Path destinationDirectory) {

    private List<String> modules() {
      return moduleUnits.stream().map(ModuleUnit::name).toList();
    }

    private Space units(List<ModuleUnit> units) {
      return new Space(
          name, List.copyOf(units), moduleSourcePaths, modulePaths, destinationDirectory);
    }

    private Space withUnit(ModuleUnit unit) {
      var units = new ArrayList<>(this.moduleUnits);
      units.add(unit);
      return units(units);
    }

    /** Copy-and-add one or more elements to the specified component. */
    private Space withModulePath(Path path, Path... more) {
      var modulePaths = new ArrayList<>(this.modulePaths);
      modulePaths.add(path);
      if (more.length > 0) modulePaths.addAll(List.of(more));
      return new Space(
          name, moduleUnits, moduleSourcePaths, List.copyOf(modulePaths), destinationDirectory);
    }
  }

  /** Conventional module information unit. */
  record ModuleUnit(String name, Optional<String> main, List<Path> resources) {
    public static ModuleUnit named(String name) {
      return new ModuleUnit(name, Optional.empty(), List.of());
    }

    public ModuleUnit main(String main) {
      return new ModuleUnit(name, Optional.ofNullable(main), resources);
    }

    public ModuleUnit resource(Path path, Path... more) {
      var resources = new ArrayList<>(this.resources);
      resources.add(path);
      if (more.length > 0) resources.addAll(List.of(more));
      return new ModuleUnit(name, main, resources);
    }
  }

  /** Conventional project builder. */
  record Builder(Bach bach, Space space) implements Conventional {

    public Builder dependentSpace(String name) {
      if (name.equals(space.name.orElse("?")))
        throw new IllegalArgumentException("name collision: " + name);

      var dependent = new BuilderFactory(bach).conventional(name).space();
      return new Builder(bach, dependent.withModulePath(getModulesDirectory()));
    }

    public Builder withModule(String name, String... more) {
      var units = new ArrayList<ModuleUnit>();
      units.add(ModuleUnit.named(name));
      for (var next : more) units.add(ModuleUnit.named(next));
      return new Builder(bach, space.units(units));
    }

    public Builder withModule(String name, UnaryOperator<ModuleUnit> operator) {
      return new Builder(bach, space.withUnit(operator.apply(ModuleUnit.named(name))));
    }

    public void grab(Grabber grabber, String... externalModules) {
      grabber.grabExternalModules(externalModules);
      grabber.grabMissingExternalModules();
    }

    public void compile() {
      compile(ToolCall.Composer.identity());
    }

    public void compile(ToolCall.Composer javacComposer) {
      compile(javacComposer, ToolCall.Composer.identity());
    }

    public void compile(ToolCall.Composer javacComposer, ToolCall.Composer jarComposer) {
      var size = space.moduleUnits.size();
      var plurals = size == 1 ? "" : "s";
      var name = space.name.orElse("Java");
      bach.logCaption("Compile %d conventional %s module%s".formatted(size, name, plurals));

      var javac = ToolCall.of("javac");
      javac.with("--module", String.join(",", space.modules()));
      javac.with("--module-source-path", String.join(File.pathSeparator, space.moduleSourcePaths));
      if (!getModulePaths().isEmpty()) javac.with("--module-path", getModulePaths());
      javac.with("-d", getClassesDirectory());
      bach.run(javacComposer.apply(javac));

      var jars = new ArrayList<ToolCall>();
      for (var unit : space.moduleUnits) jars.add(generateJarCall(unit, jarComposer));
      bach.run("directories", dir -> dir.with("clean").with(getModulesDirectory()));
      jars.parallelStream().forEach(bach::run);
    }

    public void document(ToolCall.Composer javadocComposer) {
      var api = bach.path().workspace(space.name.orElse(""), "documentation", "api");
      var javadoc = ToolCall.of("javadoc");
      javadoc.with("--module", String.join(",", space.modules()));
      javadoc.with(
          "--module-source-path", String.join(File.pathSeparator, space.moduleSourcePaths));
      if (!getModulePaths().isEmpty()) javadoc.with("--module-path", getModulePaths());
      javadoc.with("-d", api);
      bach.run(javadocComposer.apply(javadoc));

      var jar =
          ToolCall.of("jar")
              .with("--create")
              .with("--file", api.getParent().resolve("api.zip"))
              .with("--no-manifest")
              .with("-C", api, ".");
      bach.run(jar);
    }

    public void link(ToolCall.Composer jlinkComposer) {
      var image = bach.path().workspace(space.name.orElse(""), "image");
      var jlink = ToolCall.of("jlink");
      jlink.with("--output", image);
      jlink.with("--add-modules", String.join(",", space.modules()));
      jlink.with("--module-path", List.of(getModulesDirectory(), bach.path().externalModules()));
      bach.run("directories", run -> run.with("delete").with(image));
      bach.run(jlinkComposer.apply(jlink));
    }

    public void runAllTests() {
      var name = space.name.orElse("this");
      bach.logCaption("Run all tests in %s space".formatted(name));
      var moduleFinder = getRuntimeModuleFinder();
      for (var module : space.modules()) {
        if (ModuleFinderSupport.findMainClass(moduleFinder, module).isPresent()) {
          runModule(moduleFinder, module, ToolCall.Composer.identity(), bach.printer()::print);
        }
        var toolFinder = ToolFinder.of(moduleFinder, true, module);
        runTool(toolFinder, "test", ToolCall.Composer.identity(), bach.printer()::print);
        if (toolFinder.find("junit").isPresent()) runJUnit(toolFinder, module);
      }
    }

    public void runModule(String module, ToolCall.Composer composer) {
      runModule(getRuntimeModuleFinder(), module, composer, bach.printer()::print);
    }

    public void runModule(
        ModuleFinder finder, String module, ToolCall.Composer composer, ToolRun.Visitor verifier) {
      var call = composer.apply(ToolCall.module(finder, module));
      var run = bach.run(call);
      verifier.accept(run);
    }

    public void runTool(String tool, ToolCall.Composer composer) {
      var toolFinder = ToolFinder.of(getRuntimeModuleFinder(), true);
      runTool(toolFinder, tool, composer, bach.printer()::print);
    }

    public void runTool(
        ToolFinder finder, String tool, ToolCall.Composer composer, ToolRun.Visitor visitor) {
      for (var provider : finder.list(tool)) {
        var singleton = ToolFinder.of(provider);
        var call = composer.apply(ToolCall.of(singleton, tool));
        var run = bach.run(call);
        visitor.accept(run);
      }
    }

    public void runJUnit(String module) {
      var toolFinder = ToolFinder.of(getRuntimeModuleFinder(), true, module);
      runJUnit(toolFinder, module);
    }

    public void runJUnit(ToolFinder finder, String module) {
      runJUnit(finder, module, ToolCall.Composer.identity(), ToolRun.Visitor.noop());
    }

    public void runJUnit(
        ToolFinder finder, String module, ToolCall.Composer composer, ToolRun.Visitor visitor) {
      var reports = bach.path().workspace(space.name.orElse(""), "reports", "junit", module);
      var junit =
          ToolCall.of(finder, "junit")
              .with("--select-module", module)
              .with("--reports-dir", reports);
      var call = composer.apply(junit);
      var run = bach.run(call);
      visitor.accept(run);
    }

    private ToolCall generateJarCall(ModuleUnit unit, ToolCall.Composer jarComposer) {
      var file = unit.name + ".jar";
      var jar = ToolCall.of("jar");
      jar.with("--create");
      jar.with("--file", getModulesDirectory().resolve(file));
      unit.main.ifPresent(main -> jar.with("--main-class", main));
      var composed = jarComposer.apply(jar);
      composed.with("-C", getClassesDirectory().resolve(unit.name), ".");
      for (var resource : unit.resources) {
        var path = resource.isAbsolute() ? resource : bach.path().root().resolve(resource);
        if (Files.isDirectory(path)) composed.with("-C", path, ".");
        else composed.with(path);
      }
      return composed;
    }

    private List<Path> getModulePaths() {
      var paths = new ArrayList<>(space.modulePaths());
      var externalModules = bach.path().externalModules();
      if (Files.isDirectory(externalModules) && !paths.contains(externalModules))
        paths.add(externalModules);
      return List.copyOf(paths);
    }

    private Path getClassesDirectory() {
      return bach.path().workspace(space.name.orElse(""), "classes");
    }

    private Path getModulesDirectory() {
      return bach.path().workspace().resolve(space.destinationDirectory);
    }

    private ModuleFinder getRuntimeModuleFinder() {
      var paths = new ArrayList<Path>();
      paths.add(getModulesDirectory());
      paths.addAll(getModulePaths());
      return ModuleFinder.of(paths.toArray(Path[]::new));
    }
  }

  /** {@return a build program for the conventional unnamend space} */
  static String generateUnnamedSpaceBuildProgram() {
    var names =
        PathSupport.find(Path.of(""), 99, PathSupport::isModuleInfoJavaFile).stream()
            .map(ModuleDescriptorSupport::parse)
            .map(ModuleDescriptor::name)
            .sorted()
            .toList();
    var modules =
        names.isEmpty()
            ? "/* here be one or more module names */"
            : names.stream().map(name -> '"' + name + '"').collect(Collectors.joining(", "));
    return """
          import com.github.sormuras.bach.*;

          class build {
            public static void main(String... args) {
              try (var bach = new Bach(args)) {
                var space = bach.builder().conventional().withModule(%s);
                space.compile(javac -> javac.with("-Xlint").with("-Werror"), jar -> jar.with("--verbose"));
                // space.runModule("MODULE[/MAINCLASS]", run -> run.with("--dry-run"));
                // space.document(javadoc -> javadoc.with("-notimestamp").with("-Xdoclint:-missing"));
                // space.link(jlink -> jlink.with("--launcher", "NAME=MODULE[/MAINCLASS]"));
              }
            }
          }
          """
        .formatted(modules);
  }

  /** {@return a build program for a project using {@code main} and {@code test} space layout} */
  static String generateMainAndTestSpaceBuildProgram() {
    var mainModules = "/* here be one or more main modules */";
    var testModules = "/* here be one or more test modules */";
    return """
          import com.github.sormuras.bach.*;

          class build {
            public static void main(String... args) {
              try (var bach = new Bach(args)) {

                var main = bach.builder().conventional("main").withModule(%s);
                main.compile(javac -> javac.with("-Xlint").with("-Werror"), jar -> jar.with("--verbose"));

                bach.logCaption("Perform automated checks");
                var test = main.dependentSpace("test").withModule(%s);
                // test.grab(bach.grabber(...), "org.junit.jupiter", "org.junit.platform.console");
                test.compile(javac -> javac.with("-g").with("-parameters"));
                test.runAllTests();

                // bach.logCaption("Document API and link main modules into a custom runtime image");
                // main.document(javadoc -> javadoc.with("-notimestamp").with("-Xdoclint:-missing"));
                // main.link(jlink -> jlink.with("--launcher", "NAME=MODULE[/MAINCLASS]"));
              }
            }
          }
          """
        .formatted(mainModules, testModules);
  }
}
