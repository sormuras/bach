package test.base.magnificat;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.TreeMap;
import test.base.magnificat.api.Action;
import test.base.magnificat.api.Project;
import test.base.magnificat.core.UnsupportedActionException;
import test.base.magnificat.internal.Strings;

public record Bach(Configuration configuration, Project project) {

  public static Bach of(String... args) {
    var printer = Printer.ofSystem();
    return of(printer, args);
  }

  public static Bach of(Printer printer, String... args) {
    var options = Options.ofCommandLineArguments(args);
    return of(printer, options);
  }

  public static Bach of(Printer printer, Options options) {
    var configuration = new Configurator(printer).configure(options);
    return of(configuration);
  }

  public static Bach of(Configuration configuration) {
    var project = configuration.binding().newProjectFactory(configuration).newProject();
    return new Bach(configuration, project);
  }

  public static String version() {
    var module = Bach.class.getModule();
    if (!module.isNamed()) throw new IllegalStateException("Bach's module is unnamed?!");
    return module.getDescriptor().version().map(Object::toString).orElse("exploded");
  }

  static Path location() {
    var module = Bach.class.getModule();
    if (!module.isNamed()) throw new IllegalStateException("Bach's module is unnamed?!");
    var resolved = module.getLayer().configuration().findModule(module.getName()).orElseThrow();
    var uri = resolved.reference().location().orElseThrow();
    return Path.of(uri);
  }

  public Bach {
    // components are not assigned, yet -> use constructor's parameters directly
    configuration.logbook().log(System.Logger.Level.INFO, "🎼 Bach " + version());
    configuration.logbook().log(System.Logger.Level.INFO, location().toString());
  }

  public void say(String message) {
    configuration().logbook().log(System.Logger.Level.INFO, message);
  }

  public void log(String message) {
    configuration().logbook().log(System.Logger.Level.DEBUG, message);
  }

  int run() {
    say(Strings.generateBanner());
    new TreeMap<>(configuration().options().map())
        .forEach((o, v) -> say(String.format("  %-20s -> %s", o.name(), String.join(" ", v))));

    var start = Instant.now();
    try {
      configuration().options().actions().forEach(this::run);
    } catch (Exception exception) {
      configuration().logbook().log(exception);
      return 1;
    } finally {
      say("Run took " + Strings.toString(Duration.between(start, Instant.now())));
      writeLogbook();
    }
    return 0;
  }

  void run(Action action) {
    log(String.format("Run %s action", action));
    switch (action) {
      case BUILD -> build();
      case CLEAN -> clean();
      case COMPILE_MAIN -> compileMainCodeSpace();
      case COMPILE_TEST -> compileTestCodeSpace();
      case EXECUTE_TESTS -> executeTests();
      default -> throw new UnsupportedActionException(action.toString());
    }
  }

  public void build() {
    configuration().binding().newBuildAction(this).build();
  }

  public void clean() {
    configuration.binding().newCleanAction(this).clean();
  }

  public void compileMainCodeSpace() {
    configuration().binding().newCompileMainCodeSpaceAction(this).compile();
  }

  public void compileTestCodeSpace() {
    configuration().binding().newCompileTestCodeSpaceAction(this).compile();
  }

  public void executeTests() {
    configuration().binding().newExecuteTestsAction(this).execute();
  }

  public void writeLogbook() {
    configuration().binding().newWriteLogbookAction(this).write();
  }
}
