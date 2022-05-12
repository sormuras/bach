package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.ArgumentsParser;
import com.github.sormuras.bach.internal.ArgumentsParser.Opt;
import com.github.sormuras.bach.internal.ModuleDescriptorSupport;
import com.github.sormuras.bach.internal.ModuleLayerSupport;
import com.github.sormuras.bach.project.DeclaredModule;
import com.github.sormuras.bach.project.DeclaredModules;
import com.github.sormuras.bach.project.Project;
import java.io.PrintWriter;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import jdk.jfr.Recording;

/** Bach's main program. */
public final class Main implements ToolProvider {

  /** Bach's command-line interface. */
  public record Arguments(
      Optional<Boolean> help,
      Optional<Boolean> verbose,
      Optional<Boolean> dry_run,
      // Basic Properties
      Optional<String> root_directory,
      Optional<String> output_directory,
      Optional<String> module_info_find_pattern,
      // Project Properties
      Optional<String> project_name,
      Optional<String> project_version,
      Optional<String> project_version_date,
      Optional<String> project_targets_java,
      Optional<String> project_launcher,
      // Initial Tool Call
      @Opt(help = "The initial tool call: TOOL-NAME [TOOL-ARGS...]") List<String> command) {}

  /** Run an initial tool call and terminate the currently running VM on any error. */
  public static void main(String... args) {
    var code = new Main().run(Printer.ofSystem(), args);
    if (code != 0) System.exit(code);
  }

  /** {@return an instance of {@code Bach} using the printer and configured by the arguments} */
  public static Bach bach(Printer printer, String... args) {
    var parser = ArgumentsParser.create(Arguments.class);
    var arguments = parser.parse(true, args);
    return bach(printer, parser, arguments);
  }

  private static Bach bach(
      Printer printer, ArgumentsParser<Arguments> parser, Arguments arguments) {
    var paths = Paths.ofRoot(arguments.root_directory().orElse(""));
    var file = loadFileArguments(paths, "project-info.args", parser);
    var layer = loadModuleLayer(paths, "project-info", printer);

    var finder =
        ToolFinder.compose(
            ToolFinder.of(layer),
            ToolFinder.ofModularTools(paths.externalModules()),
            ToolFinder.ofJavaTools(paths.externalTools()),
            ToolFinder.ofSystemTools(),
            ToolFinder.ofNativeToolsInJavaHome("jarsigner", "java", "jdeprscan", "jfr"));

    var flags = EnumSet.noneOf(Flag.class);
    arguments.verbose().ifPresent(verbose -> flags.add(Flag.VERBOSE));
    arguments.dry_run().ifPresent(verbose -> flags.add(Flag.DRY_RUN));

    var configurators =
        ServiceLoader.load(layer, Configurator.class).stream()
            .map(ServiceLoader.Provider::get)
            .toList();
    if (configurators.size() >= 2)
      throw new RuntimeException("Too many configurators: " + configurators);

    var configuration =
        Configuration.ofDefaults().with(printer).with(paths).with(new Flags(flags)).with(finder);

    var pattern =
        arguments
            .module_info_find_pattern()
            .or(file::module_info_find_pattern)
            .orElse("glob:**/module-info.java");

    var project = Project.ofDefaults();
    project = withWalkingDirectory(project, paths.root(), pattern);
    project = withApplyingArguments(project, file);
    for (var configurator : configurators) {
      project = withApplyingConfigurator(project, configurator);
    }
    project = withApplyingArguments(project, arguments);
    return new Bach(configuration, project);
  }

  public Main() {}

  @Override
  public String name() {
    return "bach";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    return run(new Printer(out, err), args);
  }

  /** {@return the result of running the initial tool call} */
  private int run(Printer printer, String... args) {
    var parser = ArgumentsParser.create(Arguments.class);
    var arguments = parser.parse(true, args);
    if (arguments.help().orElse(false)) {
      printer.out(parser.toHelp(name()));
      return 0;
    }
    if (arguments.command().isEmpty()) {
      printer.out(parser.toHelp(name()));
      printer.err("No initial command");
      return 1;
    }
    var bach = bach(printer, parser, arguments);
    return run(bach, ToolCall.of(arguments.command()));
  }

  /** {@return the result of running the initial tool call while recording events} */
  private int run(Bach bach, ToolCall call) {
    var printer = bach.configuration().printer();
    var verbose = bach.configuration().isVerbose();
    var output = bach.configuration().paths().out();
    try (var recording = new Recording()) {
      recording.start();
      try {
        if (verbose) printer.out("BEGIN");
        bach.run(call);
        if (verbose) printer.out("END.");
        return 0;
      } catch (RuntimeException exception) {
        printer.err(exception.getClass().getSimpleName() + ": " + exception.getMessage());
        exception.printStackTrace(printer.err());
        return 2;
      } finally {
        recording.stop();
        var jfr = Files.createDirectories(output).resolve("bach-logbook.jfr");
        recording.dump(jfr);
      }
    } catch (Exception exception) {
      exception.printStackTrace(printer.err());
      return -2;
    }
  }

  static Arguments loadFileArguments(Paths paths, String name, ArgumentsParser<Arguments> parser) {
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

  static ModuleLayer loadModuleLayer(Paths paths, String name, Printer printer) {
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
    return configurator.configure(project);
  }

  static Project withApplyingArguments(Project project, Main.Arguments arguments) {
    var it = new AtomicReference<>(project);
    arguments.project_name().ifPresent(name -> it.set(it.get().withName(name)));
    arguments.project_version().ifPresent(version -> it.set(it.get().withVersion(version)));
    arguments.project_version_date().ifPresent(date -> it.set(it.get().withVersionDate(date)));
    arguments.project_targets_java().ifPresent(java -> it.set(it.get().withTargetsJava(java)));
    arguments.project_launcher().ifPresent(launcher -> it.set(it.get().withLauncher(launcher)));
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
