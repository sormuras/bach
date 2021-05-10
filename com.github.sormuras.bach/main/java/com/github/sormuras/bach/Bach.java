package com.github.sormuras.bach;

import com.github.sormuras.bach.api.Action;
import com.github.sormuras.bach.api.Project;
import com.github.sormuras.bach.api.ProjectInfo;
import com.github.sormuras.bach.api.UnsupportedActionException;
import com.github.sormuras.bach.core.Commander;
import com.github.sormuras.bach.core.CoreTrait;
import com.github.sormuras.bach.core.ExternalModuleTrait;
import com.github.sormuras.bach.core.HttpTrait;
import com.github.sormuras.bach.core.PrintTrait;
import com.github.sormuras.bach.internal.BachInfoModuleBuilder;
import com.github.sormuras.bach.internal.Strings;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Consumer;

public record Bach(Logbook logbook, Options options, Factory factory, Project project)
    implements CoreTrait, HttpTrait, PrintTrait, ExternalModuleTrait, Commander {

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
    return new Bach(logbook, options, factory, project);
  }

  public Bach bach() {
    return this;
  }

  public void say(String message) {
    logbook.log(System.Logger.Level.INFO, message);
  }

  public void log(String message) {
    logbook.log(System.Logger.Level.DEBUG, message);
  }

  public int run() {
    if (options.version()) return exit(Strings.version());
    if (options.help()) return exit(Options.generateHelpMessage(Options::isHelp));
    if (options.helpExtra()) return exit(Options.generateHelpMessage(Options::isHelpExtra));
    if (options.listConfiguration()) return exit(options.toString());
    if (options.listModules()) return exit(this::printModules);
    if (options.listTools()) return exit(this::printTools);
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
    return runActions(options.actions());
  }

  private int exit(String message) {
    logbook.printer().out().println(message);
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

  private int runActions(List<Action> actions) {
    if (options.verbose()) {
      say("Configuration");
      say(options.toString());
    }

    say(project.name() + " " + project.version());
    var start = Instant.now();
    try {
      actions.forEach(this::runAction);
    } catch (Exception exception) {
      logbook.log(exception);
      return 1;
    } finally {
      say("Bach run took " + Strings.toString(Duration.between(start, Instant.now())));
      writeLogbook();
    }
    return 0;
  }

  private void runAction(Action action) {
    log("run(%s)".formatted(action));
    switch (action) {
      case BUILD -> build();
      case CLEAN -> clean();
      case COMPILE_MAIN -> compileMainCodeSpace();
      case COMPILE_TEST -> compileTestCodeSpace();
      case EXECUTE_TESTS -> executeTests();
      case WRITE_LOGBOOK -> writeLogbook();
      default -> throw new UnsupportedActionException(action.toString());
    }
  }
}
