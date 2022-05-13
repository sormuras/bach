package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.ArgumentsParser;
import com.github.sormuras.bach.internal.ModuleDescriptorSupport;
import com.github.sormuras.bach.internal.ModuleLayerSupport;
import com.github.sormuras.bach.project.DeclaredModule;
import com.github.sormuras.bach.project.DeclaredModules;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

record MainBachBuilder(Printer printer, ArgumentsParser<CommandLineInterface> parser) {

  MainBachBuilder(Printer printer) {
    this(printer, ArgumentsParser.create(CommandLineInterface.class));
  }

  Bach build(String... args) {
    return build(parser.parse(true, args));
  }

  Bach build(CommandLineInterface commandLineArguments) {
    var flags = EnumSet.noneOf(Flag.class);
    commandLineArguments.verbose().ifPresent(__ -> flags.add(Flag.VERBOSE));
    commandLineArguments.dry_run().ifPresent(__ -> flags.add(Flag.DRY_RUN));

    var paths = Paths.ofRoot(commandLineArguments.root_directory().orElse(""));
    var fileArguments = loadFileArguments(paths, "project-info.args");
    var projectInfoLayer = loadModuleLayer(paths, "project-info");

    var configurator = loadConfigurator(projectInfoLayer);

    var configuration =
        Configuration.ofDefaults()
            .with(printer)
            .with(paths)
            .with(new Flags(flags))
            .with(configurator.configureToolFinder(paths));

    var pattern =
        commandLineArguments
            .module_info_find_pattern()
            .or(fileArguments::module_info_find_pattern)
            .orElse("glob:**/module-info.java");

    var project = Project.ofDefaults();
    project = withWalkingDirectory(project, paths.root(), pattern);
    project = withApplyingArguments(project, fileArguments);
    project = withApplyingConfigurator(project, configurator);
    project = withApplyingArguments(project, commandLineArguments);
    return new Bach(configuration, project);
  }

  Configurator loadConfigurator(ModuleLayer layer) {
    var configurators =
        ServiceLoader.load(layer, Configurator.class).stream()
            .map(ServiceLoader.Provider::get)
            .toList();
    if (configurators.size() >= 2)
      throw new RuntimeException("Too many configurators: " + configurators);
    return configurators.isEmpty() ? Configurator.identity() : configurators.get(0);
  }

  CommandLineInterface loadFileArguments(Paths paths, String name) {
    var files =
        Stream.of(paths.root(name), paths.root(".bach", name))
            .filter(Files::isRegularFile)
            .toList();
    if (files.isEmpty()) return parser.parse();
    if (files.size() > 1) throw new RuntimeException("Expected single file:" + files);
    var file = files.get(0);
    try {
      var lines =
          Files.readAllLines(file).stream()
              .map(String::strip)
              .filter(line -> !line.startsWith("#"));
      return parser.parse(lines.toArray(String[]::new));
    } catch (Exception exception) {
      throw new RuntimeException("Read all lines from file failed: " + file, exception);
    }
  }

  ModuleLayer loadModuleLayer(Paths paths, String name) {
    var info = Path.of(name);
    var directories =
        Stream.of("", ".bach")
            .map(base -> paths.root(base).resolve(info))
            .map(Path::normalize)
            .filter(Files::isDirectory)
            .filter(directory -> Files.isRegularFile(directory.resolve("module-info.java")))
            .toList();
    if (directories.isEmpty()) return ModuleLayer.empty();
    if (directories.size() > 1) throw new RuntimeException("Expected single folder:" + directories);
    var directory = directories.get(0);
    try {
      var module = ModuleDescriptorSupport.parse(directory.resolve("module-info.java"));
      var javac = ToolProvider.findFirst("javac").orElseThrow();
      var location = Bach.class.getProtectionDomain().getCodeSource().getLocation().toURI();
      var target = paths.out(info.getFileName().toString());
      javac.run(
          printer.out(),
          printer.err(),
          "--module=" + module.name(),
          "--module-source-path=" + module.name() + '=' + directory,
          "--module-path=" + Path.of(location),
          "-d",
          target.toString());
      return ModuleLayerSupport.layer(ModuleFinder.of(target), false);
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  static Project withApplyingConfigurator(Project project, Configurator configurator) {
    return configurator.configureProject(project);
  }

  static Project withApplyingArguments(Project project, CommandLineInterface cli) {
    var it = new AtomicReference<>(project);
    cli.project_name().ifPresent(name -> it.set(it.get().withName(name)));
    cli.project_version().ifPresent(version -> it.set(it.get().withVersion(version)));
    cli.project_version_date().ifPresent(date -> it.set(it.get().withVersionDate(date)));
    cli.project_targets_java().ifPresent(java -> it.set(it.get().withTargetsJava(java)));
    cli.project_launcher().ifPresent(launcher -> it.set(it.get().withLauncher(launcher)));
    return it.get();
  }

  /**
   * {@return new project instance configured by finding {@code module-info.java} files matching the
   * given {@link java.nio.file.FileSystem#getPathMatcher(String) syntaxAndPattern} below the
   * specified root directory}
   */
  static Project withWalkingDirectory(Project project, Path directory, String syntaxAndPattern) {
    var name = directory.normalize().toAbsolutePath().getFileName();
    if (name != null) project = project.withName(name.toString());
    var matcher = directory.getFileSystem().getPathMatcher(syntaxAndPattern);
    try (var stream = Files.find(directory, 9, (p, a) -> matcher.matches(p))) {
      var inits = DeclaredModules.of();
      var mains = DeclaredModules.of();
      var tests = DeclaredModules.of();
      for (var path : stream.toList()) {
        var uri = path.toUri().toString();
        if (uri.contains("/.bach/")) continue;
        var module = DeclaredModule.of(path);
        if (uri.contains("/init/")) {
          inits = inits.with(module);
          continue;
        }
        if (uri.contains("/test/")) {
          tests = tests.with(module);
          continue;
        }
        mains = mains.with(module);
      }
      project = project.with(project.spaces().init().withModules(inits));
      project = project.with(project.spaces().main().withModules(mains));
      project = project.with(project.spaces().test().withModules(tests));
    } catch (Exception exception) {
      throw new RuntimeException("Find with %s failed".formatted(syntaxAndPattern), exception);
    }
    return project;
  }
}
