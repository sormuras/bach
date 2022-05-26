package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.CommandLineInterface;
import com.github.sormuras.bach.Configuration;
import com.github.sormuras.bach.Configurator;
import com.github.sormuras.bach.Flag;
import com.github.sormuras.bach.Paths;
import com.github.sormuras.bach.Printer;
import com.github.sormuras.bach.Project;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

public record MainSupport(Printer printer, ArgVester<CommandLineInterface> parser) {

  public MainSupport(Printer printer) {
    this(printer, ArgVester.create(CommandLineInterface.class));
  }

  public Bach bach(String... args) {
    return bach(parser.parse(args));
  }

  public Bach bach(CommandLineInterface commandLineArguments) {
    var verbose = commandLineArguments.verbose().orElse(false);
    if (verbose) {
      printer.out("System Properties");
      Stream.of("user.dir", "java.home", "java.version")
          .sorted()
          .map(key -> "%20s = %s".formatted(key, System.getProperty(key)))
          .forEach(printer::out);
    }

    var root = Path.of(commandLineArguments.root_directory().orElse("")).normalize();
    var paths =
        new Paths(
            root,
            commandLineArguments
                .output_directory()
                .map(Path::of)
                .orElse(root.resolve(".bach/out"))
                .normalize());

    var fileArguments = loadFileArguments(paths, "project-info.args");
    var projectInfoLayer = loadModuleLayer(paths, "project-info");

    var configurator = loadConfigurator(projectInfoLayer);

    var flags =
        configurator
            .configureFlags()
            .with(
                Flag.VERBOSE,
                commandLineArguments.verbose().or(fileArguments::verbose).orElse(false))
            .with(
                Flag.DRY_RUN,
                commandLineArguments.dry_run().or(fileArguments::dry_run).orElse(false));

    var finder = configurator.configureToolFinder(paths);
    var tweak = configurator.configureToolCallTweak();

    var configuration = new Configuration(printer, flags, paths, finder, tweak);

    if (verbose) {
      printer.out("%s".formatted(configuration));
    }

    var pattern =
        commandLineArguments
            .module_info_find_pattern()
            .or(fileArguments::module_info_find_pattern)
            .orElse("glob:**module-info.java");

    var project = configurator.configureProject(configuration, pattern);
    project = withApplyingArguments(project, fileArguments);
    project = withApplyingArguments(project, commandLineArguments);

    if (verbose) {
      printer.out("%s".formatted(project));
    }

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

  static Project withApplyingArguments(Project project, CommandLineInterface cli) {
    var it = new AtomicReference<>(project);
    cli.project_name().ifPresent(name -> it.set(it.get().withName(name)));
    cli.project_version().ifPresent(version -> it.set(it.get().withVersion(version)));
    cli.project_version_date().ifPresent(date -> it.set(it.get().withVersionDate(date)));
    cli.project_targets_java().ifPresent(java -> it.set(it.get().withTargetsJava(java)));
    cli.project_launcher().ifPresent(launcher -> it.set(it.get().withLauncher(launcher)));
    for (var moduleAndLocation : cli.project_with_external_module()) {
      var split = moduleAndLocation.split("@");
      var name = split[0];
      var from = split[1]; // FEATURE Translate from Maven coordinates?
      it.set(it.get().withExternalModule(name, from));
    }
    for (var libraryAndVersion : cli.project_with_external_modules()) {
      var deque = new ArrayDeque<>(List.of(libraryAndVersion.split("[@:]")));
      var library = deque.removeFirst();
      var version = deque.removeFirst();
      var classifiers = deque.toArray(String[]::new);
      it.set(it.get().withExternalModules(library, version, classifiers));
    }
    return it.get();
  }
}
