package com.github.sormuras.bach;

import com.github.sormuras.bach.api.Folders;
import com.github.sormuras.bach.api.Project;
import com.github.sormuras.bach.api.ProjectInfo;
import com.github.sormuras.bach.internal.BachInfoModuleBuilder;
import com.github.sormuras.bach.internal.Strings;
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

public record Bach(Core core, Project project)
    implements WorkflowTrait, PrintTrait, ResolveTrait, ToolTrait {

  public static Bach of(String... args) {
    return Bach.of(Printer.ofSystem(), args);
  }

  public static Bach of(Printer printer, String... args) {
    var initialOptions = Options.ofCommandLineArguments(args);
    var verbose = initialOptions.verbose();
    var initialLogbook = Logbook.of(printer, verbose != null ? verbose : false);
    initialLogbook.log(System.Logger.Level.DEBUG, "Bach.of(%s)".formatted(List.of(args)));
    return Bach.of(initialLogbook, initialOptions);
  }

  public static Bach of(Logbook initialLogbook, Options initialOptions) {
    initialLogbook.log(System.Logger.Level.DEBUG, "Bach.of(%s)".formatted(initialOptions));
    var bootOptions = initialOptions.underlay(Options.ofDefaultValues());
    var root = bootOptions.chroot();
    var module = new BachInfoModuleBuilder(initialLogbook, bootOptions).build();
    var defaultInfo = Bach.class.getModule().getAnnotation(ProjectInfo.class);
    var info = Optional.ofNullable(module.getAnnotation(ProjectInfo.class)).orElse(defaultInfo);
    var options =
        initialOptions.underlay(
            Options.ofFile(root.resolve("bach.args")),
            Options.ofCommandLineArguments(Strings.unroll(info.arguments()).toList()),
            Options.ofProjectInfo(info),
            Options.ofDefaultValues());

    var logbook = initialLogbook.verbose(options.verbose());
    var service = ServiceLoader.load(module.getLayer(), Factory.class);
    var factory = service.findFirst().orElseGet(Factory::new);
    var folders = Folders.of(root);
    var core = new Core(logbook, module.getLayer(), options, factory, folders);
    var project = factory.newProjectBuilder(core).build();
    return new Bach(core, project);
  }

  public Bach bach() {
    return this;
  }

  public Logbook logbook() {
    return core.logbook();
  }

  public Options options() {
    return core.options();
  }

  public void say(String message) {
    logbook().info(message);
  }

  public void log(String message) {
    logbook().debug(message);
  }

  public int run() {
    var options = options();
    var logbook = logbook();
    if (options.version()) return exit(Strings.version());
    if (options.help()) return exit(Options.generateHelpMessage(Options::isHelp));
    if (options.help_extra()) return exit(Options.generateHelpMessage(Options::isHelpExtra));
    if (options.print_configuration()) return exit(options.toString());
    if (options.print_modules()) return exit(this::printModules);
    if (options.print_declared_modules()) return exit(this::printDeclaredModules);
    if (options.print_external_modules()) return exit(this::printExternalModules);
    if (options.print_system_modules()) return exit(this::printSystemModules);
    if (options.print_tools()) return exit(this::printTools);
    if (isPresent(options.describe_tool(), this::printToolDescription)) return 0;
    if (isPresent(options.load_external_module(), this::loadExternalModules)) return 0;
    if (options.load_missing_external_modules()) return exit(this::loadMissingExternalModules);
    if (options.tool() != null) {
      var tool = options.tool();
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

    var workflows = options.workflow();
    var start = Instant.now();
    try {
      ExtensionPoint.BeginOfWorkflowExecution.fire(this);
      workflows.forEach(this::run);
      ExtensionPoint.EndOfWorkflowExecution.fire(this);
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

  private <T> boolean isPresent(T optional, Consumer<T> consumer) {
    if (optional == null) return false;
    consumer.accept(optional);
    return true;
  }
}
