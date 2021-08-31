package com.github.sormuras.bach.builder;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Configuration;
import com.github.sormuras.bach.Grabber;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.ToolFinder;
import com.github.sormuras.bach.ToolRun;
import com.github.sormuras.bach.internal.ModuleDescriptorSupport;
import com.github.sormuras.bach.internal.ModuleFinderSupport;
import com.github.sormuras.bach.internal.PathSupport;
import java.io.File;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** An API for building modular Java projects using conventional source file tree layouts. */
public sealed interface Conventional {

  /** Conventional module space descriptor. */
  record Space(
      Optional<String> name,
      List<String> modules,
      List<String> moduleSourcePaths,
      List<Path> modulePaths,
      Path destinationDirectory) {

    /** Copy-and-set the specified component. */
    private Space modulePaths(List<Path> modulePaths) {
      return new Space(
          name, modules, moduleSourcePaths, List.copyOf(modulePaths), destinationDirectory);
    }

    /** Copy-and-add one or more elements to the specified component. */
    private Space withModulePath(Path path, Path... more) {
      var modulePaths = new ArrayList<>(this.modulePaths);
      modulePaths.add(path);
      if (more.length > 0) modulePaths.addAll(List.of(more));
      return modulePaths(List.copyOf(modulePaths));
    }
  }

  /** Conventional project builder. */
  record Builder(Bach bach, Space space) implements Conventional {

    public Builder dependentSpace(String name, String... modules) {
      if (name.equals(space.name.orElse("?")))
        throw new IllegalArgumentException("name collision: " + name);

      var dependent =
          new BuilderFactory(bach)
              .conventionalSpace(name, modules)
              .space()
              .withModulePath(getModulesDirectory());
      return new Builder(bach, dependent);
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
      var size = space.modules.size();
      var plurals = size == 1 ? "" : "s";
      var name = space.name.orElse("Java");
      bach.logCaption("Compile %d conventional %s module%s".formatted(size, name, plurals));

      var javac = ToolCall.of("javac");
      javac.with("--module", String.join(",", space.modules));
      javac.with("--module-source-path", String.join(File.pathSeparator, space.moduleSourcePaths));
      if (!getModulePaths().isEmpty()) javac.with("--module-path", getModulePaths());
      javac.with("-d", getClassesDirectory());
      bach.run(javacComposer.apply(javac));

      var jars = new ArrayList<ToolCall>();
      for (var module : space.modules) jars.add(generateJarCall(module, jarComposer));
      bach.run("directories", dir -> dir.with("clean").with(getModulesDirectory()));
      jars.parallelStream().forEach(bach::run);
    }

    public void document(ToolCall.Composer javadocComposer) {
      var api = bach.path().workspace(space.name.orElse(""), "documentation", "api");
      var javadoc = ToolCall.of("javadoc");
      javadoc.with("--module", String.join(",", space.modules));
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
      jlink.with("--add-modules", String.join(",", space.modules));
      jlink.with("--module-path", List.of(getModulesDirectory(), bach.path().externalModules()));
      bach.run("directories", run -> run.with("delete").with(image));
      bach.run(jlinkComposer.apply(jlink));
    }

    public void runAllTests() {
      var name = space.name.orElse("this");
      bach.logCaption("Run all tests in %s space".formatted(name));
      var moduleFinder = getRuntimeModuleFinder();
      for (var module : space.modules) {
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

    private ToolCall generateJarCall(String module, ToolCall.Composer jarComposer) {
      var version = findVersion(module);
      var file =
          version
              .map(value -> Configuration.computeJarFileName(module, value))
              .orElseGet(() -> module + ".jar");
      var jar = ToolCall.of("jar");
      jar.with("--create");
      jar.with("--file", getModulesDirectory().resolve(file));
      version.ifPresent(v -> jar.with("--module-version", v));
      findMainClass(module).ifPresent(mainClass -> jar.with("--main-class", mainClass));
      var composed = jarComposer.apply(jar);
      composed.with("-C", getClassesDirectory().resolve(module), ".");
      return composed;
    }

    private Optional<Version> findVersion(String module) {
      return Optional.empty();
    }

    private Optional<String> findMainClass(String module) {
      var name = module + ".Main";
      var path = Path.of(name.replace('.', '/') + ".class");
      var file = getClassesDirectory().resolve(module).resolve(path);
      return Files.exists(file) ? Optional.of(name) : Optional.empty();
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
                var space = bach.builder().conventional(%s);
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

                var main = bach.builder().conventionalSpace("main", %s);
                main.compile(javac -> javac.with("-Xlint").with("-Werror"), jar -> jar.with("--verbose"));

                bach.logCaption("Perform automated checks");
                var test = main.dependentSpace("test", %s);
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
