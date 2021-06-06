package com.github.sormuras.bach;

import com.github.sormuras.bach.api.Folders;
import com.github.sormuras.bach.api.Project;
import com.github.sormuras.bach.api.ProjectInfo;
import com.github.sormuras.bach.internal.BachInfoModuleBuilder;
import com.github.sormuras.bach.internal.Strings;
import com.github.sormuras.bach.internal.ToolProviders;
import com.github.sormuras.bach.trait.PrintTrait;
import com.github.sormuras.bach.trait.ResolveTrait;
import com.github.sormuras.bach.trait.ToolTrait;
import com.github.sormuras.bach.trait.WorkflowTrait;
import java.lang.module.ModuleFinder;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.Optional;
import java.util.ServiceLoader;

public record Bach(Core core, Project project)
    implements WorkflowTrait, PrintTrait, ResolveTrait, ToolTrait {

  public static int run(Printer printer, Options initialOptions) {
    var options = initialOptions.underlay(Options.ofDefaultValues());
    var command = options.command();
    var out = printer.out();
    switch (command.name()) {
      case NOOP -> {
      }
      case PRINT_VERSION -> out.println(Bach.version());
      case PRINT_HELP -> out.println(Options.generateHelpMessage(Options::isHelp));
      case PRINT_HELP_EXTRA -> out.println(Options.generateHelpMessage(Options::isHelpExtra));
      case DESCRIBE_TOOL, RUN_TOOL -> {
        var list = new LinkedList<>(command.arguments());
        var name = list.removeFirst();
        var folders = Folders.of(options.chroot());
        var finder = ModuleFinder.of(folders.externals());
        var tool = ToolProviders.of(finder).find(name).orElseThrow();
        if (command.name() == Command.Name.DESCRIBE_TOOL) {
          out.println(ToolProviders.describe(tool));
        }
        if (command.name() == Command.Name.RUN_TOOL) {
          Thread.currentThread().setContextClassLoader(tool.getClass().getClassLoader());
          return tool.run(out, printer.err(), list.toArray(String[]::new));
        }
      }
      default -> throw new UnsupportedOperationException("Unsupported command: " + command);
    }
    if (command.name() != Command.Name.NOOP) return 0;
    return Bach.of(printer, initialOptions).run();
  }

  public static Bach of(String... args) {
    return Bach.of(Printer.ofSystem(), Options.ofCommandLineArguments(args));
  }

  public static Bach of(Printer printer, Options initialOptions) {
    var verbose = initialOptions.verbose();
    var initialLogbook = Logbook.of(printer, verbose != null ? verbose : false);
    return Bach.of(initialLogbook, initialOptions);
  }

  public static Bach of(Logbook initialLogbook, Options initialOptions) {
    initialLogbook.debug("Bach.of(%s)".formatted(initialOptions));
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

  public static String version() {
    var module = Bach.class.getModule();
    if (!module.isNamed()) throw new IllegalStateException("Bach's module is unnamed?!");
    return module.getDescriptor().version().map(Object::toString).orElse("exploded");
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
    var command = options.command();
    switch (command.name()) {
      case PRINT_MODULES -> printModules();
      case PRINT_DECLARED_MODULES -> printDeclaredModules();
      case PRINT_EXTERNAL_MODULES -> printExternalModules();
      case PRINT_SYSTEM_MODULES -> printSystemModules();
      case PRINT_TOOLS -> printTools();
      case LOAD_EXTERNAL_MODULE -> loadExternalModules(command.arguments().toArray(String[]::new));
      case LOAD_MISSING_EXTERNAL_MODULES -> loadMissingExternalModules();
    }
    if (command.name() != Command.Name.NOOP) return 0;

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
      logbook().log(exception);
      return 1;
    } finally {
      say("Bach run took " + Strings.toString(Duration.between(start, Instant.now())));
      writeLogbook();
    }
    return 0;
  }
}
