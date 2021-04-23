package com.github.sormuras.bach;

import com.github.sormuras.bach.api.Action;
import com.github.sormuras.bach.api.Option;
import com.github.sormuras.bach.api.Project;
import com.github.sormuras.bach.api.ProjectInfo;
import com.github.sormuras.bach.api.UnsupportedActionException;
import com.github.sormuras.bach.core.CoreTrait;
import com.github.sormuras.bach.core.ListTrait;
import com.github.sormuras.bach.core.ToolProviders;
import com.github.sormuras.bach.internal.BachInfoModuleBuilder;
import com.github.sormuras.bach.internal.HelpMessageBuilder;
import com.github.sormuras.bach.internal.Strings;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.ServiceLoader;

public record Bach(Logbook logbook, Options options, Plugins plugins, Project project)
    implements CoreTrait, ListTrait {

  public static Bach of(String... args) {
    return Bach.of(Printer.ofSystem(), args);
  }

  public static Bach of(Printer printer, String... args) {
    var initialOptions = Options.ofCommandLineArguments("Initial Options", args);
    var initialLogbook = Logbook.of(printer, initialOptions.is(Option.VERBOSE));
    return Bach.of(initialLogbook, initialOptions);
  }

  public static Bach of(Logbook initialLogbook, Options initialOptions) {
    var defaultOptions = Options.ofDefaultValues();
    var interimOptions = Options.compose("Interims", Logbook.of(), initialOptions, defaultOptions);
    var root = Path.of(interimOptions.get(Option.CHROOT));
    var module = new BachInfoModuleBuilder(initialLogbook, interimOptions).build();
    var defaultInfo = Bach.class.getModule().getAnnotation(ProjectInfo.class);
    var info = Optional.ofNullable(module.getAnnotation(ProjectInfo.class)).orElse(defaultInfo);
    var options =
        Options.compose(
            "Options",
            initialLogbook,
            initialOptions,
            Options.ofCommandLineArguments("ProjectInfo Arguments", info.arguments()),
            Options.ofProjectInfoOptions(info.options()),
            Options.ofProjectInfoElements(info),
            Options.ofFile(root.resolve("bach.args")),
            Options.ofDirectory(root),
            defaultOptions);

    var verbose = options.is(Option.VERBOSE);
    var logbook = new Logbook(initialLogbook.printer(), verbose, initialLogbook.messages());
    var service = ServiceLoader.load(module.getLayer(), Plugins.class);
    var plugins = service.findFirst().orElseGet(Plugins::new);
    var project = plugins.newProjectBuilder(logbook, options).build();
    return new Bach(logbook, options, plugins, project);
  }

  static String banner() {
    var location = location();
    var type = Files.isDirectory(location) ? "directory" : "modular JAR file";
    var directory = Path.of("").toAbsolutePath();
    return """
           Bach %s
             Loaded from %s %s in directory: %s
             Launched by Java %s running on %s.
             The current working directory is: %s
           """
        .formatted(
            version(),
            type,
            location.getFileName(),
            directory.relativize(location).getParent().toUri(),
            Runtime.version(),
            System.getProperty("os.name"),
            directory.toUri())
        .strip();
  }

  static String version() {
    var module = Bach.class.getModule();
    if (!module.isNamed()) throw new IllegalStateException("Bach's module is unnamed?!");
    return module.getDescriptor().version().map(Object::toString).orElse("exploded");
  }

  public static Path location() {
    var module = Bach.class.getModule();
    if (!module.isNamed()) throw new IllegalStateException("Bach's module is unnamed?!");
    var resolved = module.getLayer().configuration().findModule(module.getName()).orElseThrow();
    var uri = resolved.reference().location().orElseThrow();
    return Path.of(uri);
  }

  public Bach bach() {
    return this;
  }

  public boolean is(Option option) {
    return options().is(option);
  }

  public void say(String message) {
    logbook().log(System.Logger.Level.INFO, message);
  }

  public void log(String message) {
    logbook().log(System.Logger.Level.DEBUG, message);
  }

  public int run() {
    var out = logbook().printer().out();
    if (is(Option.VERSION)) {
      out.println(version());
      return 0;
    }
    say(banner());
    if (is(Option.SHOW_CONFIGURATION)) {
      say("Configuration");
      options().lines(Option::isStandard).forEach(this::say);
    }
    if (is(Option.HELP)) {
      out.println(new HelpMessageBuilder(Option::isStandard).build());
      return 0;
    }
    if (is(Option.HELP_EXTRA)) {
      out.println(new HelpMessageBuilder(Option::isExtra).build());
      return 0;
    }
    if (is(Option.LIST_TOOLS)) {
      listTools();
      return 0;
    }
    var tool = options().value(Option.TOOL);
    if (tool != null) {
      var line = tool.elements();
      var name = line.get(0);
      var args = line.subList(1, line.size()).toArray(String[]::new);
      var err = logbook().printer().err();
      say(name + ' ' + String.join(" ", args));
      var finder = ModuleFinder.of(project.folders().externals());
      var provider = ToolProviders.of(finder).find(name).orElseThrow();
      return provider.run(out, err, args);
    }

    say(project.name() + " 0");
    var start = Instant.now();
    try {
      options().actions().forEach(this::run);
    } catch (Exception exception) {
      logbook().log(exception);
      return 1;
    } finally {
      say("Bach run took " + Strings.toString(Duration.between(start, Instant.now())));
      writeLogbook();
    }
    return 0;
  }

  private void run(Action action) {
    log(String.format("Run %s action", action));
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
