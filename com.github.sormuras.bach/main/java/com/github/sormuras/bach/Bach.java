package com.github.sormuras.bach;

import com.github.sormuras.bach.api.Project;
import com.github.sormuras.bach.api.ProjectInfo;
import com.github.sormuras.bach.internal.BachInfoModuleBuilder;
import com.github.sormuras.bach.internal.Strings;
import com.github.sormuras.bach.trait.HttpTrait;
import com.github.sormuras.bach.trait.PrintTrait;
import com.github.sormuras.bach.trait.ResolveTrait;
import com.github.sormuras.bach.trait.ToolTrait;
import com.github.sormuras.bach.trait.WorkflowTrait;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Consumer;

public record Bach(Configuration configuration, Project project)
    implements WorkflowTrait, HttpTrait, PrintTrait, ResolveTrait, ToolTrait {

  public static Bach of(String... args) {
    return Bach.of(Printer.ofSystem(), args);
  }

  public static Bach of(Printer printer, String... args) {
    var initialOptions = Options.ofCommandLineArguments(args).id("Initial Options");
    var initialLogbook = Logbook.of(printer, initialOptions.verbose());
    initialLogbook.log(System.Logger.Level.DEBUG, "Bach.of(%s)".formatted(List.of(args)));
    return Bach.of(initialLogbook, initialOptions);
  }

  public static Bach of(Logbook initialLogbook, Options initialOptions) {
    initialLogbook.log(System.Logger.Level.DEBUG, "Bach.of(%s)".formatted(initialOptions.id()));
    var root = initialOptions.chrootOrDefault();
    var module = new BachInfoModuleBuilder(initialLogbook, initialOptions).build();
    var defaultInfo = Bach.class.getModule().getAnnotation(ProjectInfo.class);
    var info = Optional.ofNullable(module.getAnnotation(ProjectInfo.class)).orElse(defaultInfo);
    var options =
        Options.compose(
            "Options",
            initialLogbook,
            initialOptions,
            Options.ofFile(root.resolve("bach.args")),
            Options.ofCommandLineArguments(info.arguments()).id("@ProjectInfo#arguments()"),
            Options.ofProjectInfo(info));

    var logbook = initialLogbook.verbose(options.verbose());
    var service = ServiceLoader.load(module.getLayer(), Factory.class);
    var factory = service.findFirst().orElseGet(Factory::new);
    var project = factory.newProjectBuilder(logbook, options).build();
    var configuration = new Configuration(logbook, module.getLayer(), options, factory);
    return new Bach(configuration, project);
  }

  public Bach bach() {
    return this;
  }

  public Logbook logbook() {
    return configuration.logbook();
  }

  public Options options() {
    return configuration.options();
  }

  public void say(String message) {
    logbook().log(System.Logger.Level.INFO, message);
  }

  public void log(String message) {
    logbook().log(System.Logger.Level.DEBUG, message);
  }

  public int run() {
    var options = options();
    var logbook = logbook();
    if (options.version()) return exit(Strings.version());
    if (options.help()) return exit(Options.generateHelpMessage(Options::isHelp));
    if (options.helpExtra()) return exit(Options.generateHelpMessage(Options::isHelpExtra));
    if (options.printConfiguration()) return exit(options.toString());
    if (options.printModules()) return exit(this::printModules);
    if (options.printDeclaredModules()) return exit(this::printDeclaredModules);
    if (options.printExternalModules()) return exit(this::printExternalModules);
    if (options.printSystemModules()) return exit(this::printSystemModules);
    if (options.printTools()) return exit(this::printTools);
    if (isPresent(options.describeTool(), this::printToolDescription)) return 0;
    if (isPresent(options.loadExternalModule(), this::loadExternalModules)) return 0;
    if (options.loadMissingExternalModules()) return exit(this::loadMissingExternalModules);
    if (options.tool().isPresent()) {
      var tool = options.tool().get();
      var name = tool.name();
      var args = tool.arguments().toArray(String[]::new);
      var out = logbook.printer().out();
      var err = logbook.printer().err();
      var provider = findToolProvider(name).orElseThrow();
      Thread.currentThread().setContextClassLoader(provider.getClass().getClassLoader());
      return provider.run(out, err, args);
    }

    say(Strings.banner());
    if (options.verbose()) {
      say("Configuration");
      say(options.toString());
    }
    say("Work on project %s %s".formatted(project.name(), project.version()));

    var workflows = options.workflows();
    var start = Instant.now();
    try {
      Service.BeginOfWorkflowExecution.fire(this);
      workflows.forEach(this::run);
      Service.EndOfWorkflowExecution.fire(this);
    } catch (Exception exception) {
      logbook.log(exception);
      return 1;
    } finally {
      say("Bach run took " + Strings.toString(Duration.between(start, Instant.now())));
      writeLogbook();
    }
    return 0;
  }

  private int exit(String message) {
    logbook().printer().out().println(message);
    return 0;
  }

  private int exit(Runnable runnable) {
    runnable.run();
    return 0;
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private <T> boolean isPresent(Optional<T> optional, Consumer<T> consumer) {
    if (optional.isEmpty()) return false;
    consumer.accept(optional.get());
    return true;
  }
}
