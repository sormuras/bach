package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.MavenConsumerPomFilesGenerator;
import com.github.sormuras.bach.internal.Paths;
import com.github.sormuras.bach.project.CodeSpaces;
import com.github.sormuras.bach.project.Feature;
import com.github.sormuras.bach.project.MainCodeSpace;
import com.github.sormuras.bach.project.ModuleDeclaration;
import com.github.sormuras.bach.project.ModuleLookup;
import com.github.sormuras.bach.project.Project;
import com.github.sormuras.bach.project.SourceFolder;
import com.github.sormuras.bach.project.TestCodeSpace;
import com.github.sormuras.bach.tool.Command;
import com.github.sormuras.bach.tool.ToolCall;
import com.github.sormuras.bach.tool.ToolResponse;
import com.github.sormuras.bach.tool.ToolRunner;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** A modular Java project builder. */
public class Builder {

  private final Bach bach;
  private final Project project;
  private final ToolRunner runner;

  /**
   * Initialize this builder with the given components.
   *
   * @param bach the underlying Bach instance
   * @param project the project to build
   */
  public Builder(Bach bach, Project project) {
    this.bach = bach;
    this.project = project;

    this.runner = new ToolRunner(project.externals().finder());
  }

  /** @return the underlying Bach instance */
  public Bach bach() {
    return bach;
  }

  /** @return the project to build */
  public Project project() {
    return project;
  }

  /** Builds a modular Java project. */
  public void build() {
    bach.info("Build project %s %s", project.name(), project.version());
    var start = Instant.now();
    var logbook = bach.logbook();
    try {
      loadRequiredAndMissingModules();
      buildAllSpaces(project.spaces());
    } catch (Exception exception) {
      var trace = new StringWriter();
      exception.printStackTrace(new PrintWriter(trace));
      var message = "Build failed: " + exception + '\n' + trace.toString().indent(0);
      logbook.log(Level.ERROR, message);
      throw new BuildException(message);
    } finally {
      bach.info("Build took %s", Logbook.toString(Duration.between(start, Instant.now())));
      var file = logbook.write(project);
      logbook.accept("Logbook written to " + file.toUri());
    }
  }

  /** Load required and missing modules in a best-effort manner. */
  public void loadRequiredAndMissingModules() {
    bach.debug("Load required and missing modules");
    var externals = project.externals();
    var lookup = computeModuleLookup();
    externals.requires().forEach(module -> bach.loadModule(externals, lookup, module));
    bach.loadMissingModules(externals, lookup);
  }

  /**
   * Returns a module lookup composed of external module links and best-effort lookup.
   *
   * @return a module lookup
   */
  public ModuleLookup computeModuleLookup() {
    return ModuleLookup.compose(project.externals(), ModuleLookup.ofBestEffort(bach));
  }

  /**
   * Builds all code spaces.
   *
   * @param spaces the code spaces to build
   */
  public void buildAllSpaces(CodeSpaces spaces) {
    if (spaces.isEmpty()) throw new BuildException("No modules declared?!");
    buildMainCodeSpace(spaces.main());
    buildTestCodeSpace(spaces.test());
  }

  /**
   * Builds the main space.
   *
   * <ul>
   *   <li>javac + jar
   *   <li>javadoc
   *   <li>jlink
   *   <li>jpackage
   * </ul>
   *
   * @param main the main space to build
   */
  public void buildMainCodeSpace(MainCodeSpace main) {
    var modules = main.modules();
    if (modules.isEmpty()) return;
    bach.info("Compile %d main module%s", modules.size(), modules.size() == 1 ? "" : "s");

    Paths.deleteDirectories(main.workspace("modules"));
    var names = modules.toNames(", ");
    var release = main.release();
    if (release >= 9) {
      run("Compile " + names, computeMainJavacCall(release));
    } else {
      var feature = Runtime.version().feature();
      run("Pre-compile " + names, computeMainJavacCall(feature));
      buildMainSingleReleaseVintageModules(feature);
    }

    Paths.createDirectories(main.workspace("modules"));
    var targeted = new ArrayList<ToolCall>();
    var jars = new ArrayList<ToolCall>();
    for (var declaration : modules.map().values()) {
      for (var folder : declaration.sources().list()) {
        if (!folder.isTargeted()) continue;
        targeted.add(computeMainJavacCall(declaration.name(), folder));
      }
      jars.add(computeMainJarCall(declaration));
    }
    if (targeted.size() > 0) run("Compile targeted sources", targeted.stream());
    run("Create JAR files", jars.stream());

    if (isGenerateApiDocumentation()) {
      bach.info("Generate API documentation");
      run("Run javadoc for " + names, computeMainDocumentationJavadocCall());
      run("Create javadoc archive", computeMainDocumentationJarCall());
    }

    if (isGenerateCustomRuntimeImage()) {
      bach.info("Generate custom runtime image");
      Paths.deleteDirectories(main.workspace("image"));
      run("Run jlink for " + names, computeMainJLinkCall());
    }

    if (isGenerateMavenPomFiles()) {
      bach.info("Generate Maven consumer POM files");
      var generator = new MavenConsumerPomFilesGenerator(this, "  ");
      generator.execute();
    }
  }

  /**
   * @param release the release
   * @return the {@code javac} call to compile all modules of the main space
   */
  public ToolCall computeMainJavacCall(int release) {
    var main = project.spaces().main();
    return Command.builder("javac")
        .with("--release", release)
        .with("--module", main.modules().toNames(","))
        .with("--module-version", project.version())
        .with("--module-source-path", main.toModuleSourcePath())
        .with("--module-path", main.toModulePath())
        .withEach(main.tweaks().arguments("javac"))
        .with("-d", main.classes(release))
        .build();
  }

  /**
   * @param module the name of the module to compile
   * @param folder the source folder to compile
   * @return the {@code javac} call to compile a version of a multi-release module
   */
  public ToolCall computeMainJavacCall(String module, SourceFolder folder) {
    var main = project.spaces().main();
    var release = folder.release();
    var classes = main.workspace("classes-mr", release + "/" + module);
    var javaSourceFiles = new ArrayList<Path>();
    Paths.find(Path.of(module, "main/java-" + release), "**.java", javaSourceFiles::add);
    return Command.builder("javac")
        .with("--release", release)
        .with("--module-version", project.version())
        .with("--module-path", main.classes())
        .with("-implicit:none") // generate classes for explicitly referenced source files
        .withEach(main.tweaks().arguments("javac"))
        .with("-d", classes)
        .withEach(javaSourceFiles)
        .build();
  }

  /**
   * Builds all modules targeting Java 7 or Java 8.
   *
   * @param mainRelease the main classes release feature number
   */
  public void buildMainSingleReleaseVintageModules(int mainRelease) {
    var main = project.spaces().main();
    var release = main.release();
    if (release > 8) throw new IllegalStateException("release too high: " + release);

    var classPaths = new ArrayList<Path>();
    var libraries = Bach.EXTERNALS;
    main.modules().toNames().forEach(name -> classPaths.add(main.classes(mainRelease, name)));
    if (Files.isDirectory(libraries)) classPaths.addAll(Paths.list(libraries, Paths::isJarFile));

    var compiles = new ArrayList<ToolCall>();
    for (var declaration : main.modules().map().values()) {
      var moduleInfoJavaFiles = new ArrayList<Path>();
      declaration.sources().list().stream()
          .filter(SourceFolder::isModuleInfoJavaPresent)
          .forEach(folder -> moduleInfoJavaFiles.add(folder.path().resolve("module-info.java")));

      var compileModuleOnly =
          Command.builder("javac")
              .with("--release", 9)
              .with("--module-version", project.version())
              .with("--module-source-path", main.toModuleSourcePath())
              .with("--module-path", main.toModulePath())
              .with("-implicit:none") // generate classes for explicitly referenced source files
              .withEach(main.tweaks().arguments("javac"))
              .with("-d", main.classes())
              .withEach(moduleInfoJavaFiles)
              .build();
      compiles.add(compileModuleOnly);

      var module = declaration.name();
      var path = Path.of(module, "main/java");
      if (Files.notExists(path)) continue;

      var javaSourceFiles = new ArrayList<Path>();
      Paths.find(path, "**.java", javaSourceFiles::add);
      var compileSources =
          Command.builder("javac")
              .with("--release", release) // 7 or 8
              .with("--class-path", Paths.join(classPaths))
              .withEach(main.tweaks().arguments("javac"))
              .with("-d", main.classes().resolve(module))
              .withEach(javaSourceFiles)
              .build();
      compiles.add(compileSources);
    }
    run("Compile vintage sources", compiles.stream());
  }

  /**
   * @param module the module declaration to create an archive for
   * @return the {@code jar} call to archive all assets for the given module
   */
  public ToolCall computeMainJarCall(ModuleDeclaration module) {
    var main = project.spaces().main();
    var archive = computeMainJarFileName(module);
    var mainClass = module.reference().descriptor().mainClass();
    var name = module.name();
    var jar =
        Command.builder("jar")
            .with("--create")
            .with("--file", main.workspace("modules", archive))
            .with(mainClass, (builder, className) -> builder.with("--main-class", className))
            .withEach(main.tweaks().arguments("jar"))
            .withEach(main.tweaks().arguments("jar(" + name + ')'));
    // add base classes
    var baseClasses = main.classes().resolve(name);
    if (Files.isDirectory(baseClasses)) jar.with("-C", baseClasses, ".");
    // add base (re)sources
    var withSources = isIncludeSourcesInModules();
    if (module.reference().info().toString().equals("module-info.java")) {
      if (withSources) jar.with("module-info.java");
      var dot = name.indexOf('.');
      var prefix = name.substring(0, dot > 0 ? dot : name.length());
      try (var stream = Files.walk(Path.of(""))) {
        if (withSources) stream.filter(path -> path.startsWith(prefix)).forEach(jar::with);
        else
          stream // exlcude ".java" files
              .filter(path -> path.startsWith(prefix))
              .filter(path -> !path.getFileName().toString().endsWith(".java"))
              .forEach(jar::with);
      } catch (Exception ignore) {
      }
    } else {
      for (var folder : module.resources().list()) {
        if (folder.isTargeted()) continue; // handled later
        jar.with("-C", folder.path(), ".");
      }
      // add targeted classes and targeted resources in ascending order
      for (var directory : computeMainJarTargetedDirectories(module).entrySet()) {
        jar.with("--release", directory.getKey());
        for (var path : directory.getValue()) jar.with("-C", path, ".");
      }
    }
    return jar.build();
  }

  /**
   * @param module the module declaration
   * @return the name of the JAR file for the given module declaration
   */
  public String computeMainJarFileName(ModuleDeclaration module) {
    var slug = project.spaces().main().jarslug();
    var builder = new StringBuilder(module.name());
    if (!slug.isEmpty()) builder.append('@').append(slug);
    return builder.append(".jar").toString();
  }

  /**
   * @param module the module declaration
   * @return a map with "release to list-of-path" entries
   */
  public TreeMap<Integer, List<Path>> computeMainJarTargetedDirectories(ModuleDeclaration module) {
    var main = project.spaces().main();
    var assets = new TreeMap<Integer, List<Path>>();
    // targeted classes
    for (var source : module.sources().list()) {
      if (!source.isTargeted()) continue;
      var release = source.release();
      var classes = main.workspace("classes-mr", release + "/" + module.name());
      assets.merge(
          release,
          List.of(classes),
          (o, n) -> Stream.concat(o.stream(), n.stream()).collect(Collectors.toList()));
    }
    // targeted resources
    for (var resource : module.resources().list()) {
      if (!resource.isTargeted()) continue;
      var release = resource.release();
      assets.merge(
          release,
          List.of(resource.path()),
          (o, n) -> Stream.concat(o.stream(), n.stream()).collect(Collectors.toList()));
    }
    return assets;
  }

  /** @return the javadoc call generating the API documentation for all main modules */
  public ToolCall computeMainDocumentationJavadocCall() {
    var main = project.spaces().main();
    var api = main.documentation("api");
    return Command.builder("javadoc")
        .with("--module", main.modules().toNames(","))
        .with("--module-source-path", main.toModuleSourcePath())
        .with("--module-path", main.toModulePath())
        .withEach(main.tweaks().arguments("javadoc"))
        .with("-d", api)
        .build();
  }

  /** @return the jar call generating the API documentation archive */
  public ToolCall computeMainDocumentationJarCall() {
    var main = project.spaces().main();
    var api = main.documentation("api");
    var file = project.name() + "-api-" + project.version() + ".zip";
    return Command.builder("jar")
        .with("--create")
        .with("--file", api.getParent().resolve(file))
        .with("--no-manifest")
        .with("-C", api, ".")
        .build();
  }

  /** @return the jllink call */
  public ToolCall computeMainJLinkCall() {
    var main = project.spaces().main();
    var test = project.spaces().test();
    return Command.builder("jlink")
        .with("--add-modules", main.modules().toNames(","))
        .with("--module-path", test.toModulePath())
        .with(main.launcher().command(), (jlink, command) -> jlink.with("--launcher", command))
        .withEach(main.tweaks().arguments("jlink"))
        .with("--output", main.workspace("image"))
        .build();
  }

  /**
   * Builds the test space.
   *
   * <ul>
   *   <li>javac + jar
   *   <li>junit
   * </ul>
   *
   * @param test the test space to build
   */
  public void buildTestCodeSpace(TestCodeSpace test) {
    var modules = test.modules();
    if (modules.isEmpty()) return;
    bach.info("Compile %d test module%s", modules.size(), modules.size() == 1 ? "" : "s");

    Paths.deleteDirectories(test.workspace("modules-test"));

    run("Compile " + modules.toNames(", "), computeTestJavacCall());
    Paths.createDirectories(test.workspace("modules-test"));

    var declarations = test.modules().map().values();
    run("Create modular JAR files", declarations.stream().map(this::computeTestJarCall));

    if (project.externals().finder().find("org.junit.platform.console").isPresent()) {
      for (var declaration : declarations) {
        var module = declaration.name();
        var archive = module + "@" + project.version() + "+test.jar";
        var finder =
            ModuleFinder.of(
                test.workspace("modules-test", archive), // module under test
                test.workspace("modules"), // main modules
                test.workspace("modules-test"), // (more) test modules
                Bach.EXTERNALS // external modules
                );
        bach.info("Run tests in module %s", module);
        var junit = computeTestJUnitCall(declaration);
        bach.debug(junit.toCommand().toString());
        var response = runner.run(junit, finder, module);
        bach.logbook().log(response);
      }
      var errors = bach.logbook().responses(ToolResponse::isError);
      if (errors.size() > 0) {
        throw new BuildException("JUnit reported failed test module(s): " + errors.size());
      }
    }
  }

  /** @return the {@code javac} call to compile all modules of the test space. */
  public ToolCall computeTestJavacCall() {
    var main = project.spaces().main();
    var test = project.spaces().test();
    return Command.builder("javac")
        .with("--module", test.modules().toNames(","))
        .with("--module-source-path", test.toModuleSourcePath())
        .with("--module-path", test.toModulePath())
        .withEach(
            test.modules().toModulePatches(main.modules()).entrySet(),
            (javac, patch) -> javac.with("--patch-module", patch.getKey() + '=' + patch.getValue()))
        .withEach(test.tweaks().arguments("javac"))
        .with("-d", test.classes())
        .build();
  }

  /**
   * @param declaration the module declaration to create an archive for
   * @return the {@code jar} call to archive all assets for the given module
   */
  public ToolCall computeTestJarCall(ModuleDeclaration declaration) {
    var module = declaration.name();
    var archive = module + "@" + project.version() + "+test.jar";
    var test = project.spaces().test();
    return Command.builder("jar")
        .with("--create")
        .with("--file", test.workspace("modules-test", archive))
        .withEach(test.tweaks().arguments("jar"))
        .withEach(test.tweaks().arguments("jar(" + module + ')'))
        .with("-C", test.classes().resolve(module), ".")
        .build();
  }

  /**
   * @param declaration the module declaration to scan for tests
   * @return the {@code junit} call to launch the JUnit Platform for
   */
  public ToolCall computeTestJUnitCall(ModuleDeclaration declaration) {
    var module = declaration.name();
    var test = project.spaces().test();
    return Command.builder("junit")
        .with("--select-module", module)
        .with("--reports-dir", test.workspace("reports", "junit-test", module))
        .withEach(test.tweaks().arguments("junit"))
        .withEach(test.tweaks().arguments("junit(" + module + ')'))
        .build();
  }

  /** @return {@code true} if an API documenation should be generated, else {@code false} */
  public boolean isGenerateApiDocumentation() {
    return project.spaces().main().is(Feature.GENERATE_API_DOCUMENTATION);
  }

  /** @return {@code true} if a custom runtime image should be generated, else {@code false} */
  public boolean isGenerateCustomRuntimeImage() {
    return project.spaces().main().is(Feature.GENERATE_CUSTOM_RUNTIME_IMAGE);
  }

  /** @return {@code true} if a custom runtime image should be generated, else {@code false} */
  public boolean isGenerateMavenPomFiles() {
    return project.spaces().main().is(Feature.GENERATE_MAVEN_POM_FILES);
  }

  /** @return {@code true} if a custom runtime image should be generated, else {@code false} */
  public boolean isIncludeSourcesInModules() {
    return project.spaces().main().is(Feature.INCLUDE_SOURCES_IN_MODULAR_JAR);
  }

  /**
   * Runs the given tool call using the given description.
   *
   * @param description the description to log
   * @param call the tool call to run
   */
  public void run(String description, ToolCall call) {
    bach.info(description);
    run(call).checkSuccessful();
  }

  /**
   * Runs tool calls of the given stream in parallel.
   *
   * @param description the description to log
   * @param calls the tool calls to run
   */
  public void run(String description, Stream<ToolCall> calls) {
    bach.info(description);
    calls.parallel().forEach(this::run);
    var errors = bach.logbook().responses(ToolResponse::isError);
    if (errors.size() > 0) throw new BuildException("Run failed due to: " + errors);
  }

  private ToolResponse run(ToolCall call) {
    bach.debug(call.toCommand().toString());
    var response = runner.run(call);
    bach.logbook().log(response);
    return response;
  }
}
