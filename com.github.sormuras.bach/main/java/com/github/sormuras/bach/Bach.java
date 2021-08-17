package com.github.sormuras.bach;

import static com.github.sormuras.bach.Note.caption;
import static com.github.sormuras.bach.Note.message;

import com.github.sormuras.bach.internal.DurationSupport;
import com.github.sormuras.bach.internal.ExecuteModuleToolProvider;
import com.github.sormuras.bach.internal.ExecuteProcessToolProvider;
import com.github.sormuras.bach.internal.ModuleSupport;
import com.github.sormuras.bach.internal.ToolProviderSupport;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

public class Bach implements AutoCloseable {

  public static String version() {
    return ModuleSupport.version(Bach.class.getModule());
  }

  private final Configuration configuration;
  private final Logbook logbook;

  public Bach(String... args) {
    this(Configuration.of().with(Options.of(args)));
  }

  public Bach(Configuration configuration) {
    this.configuration = configuration;
    this.logbook = constructLogbook();
    log(
        "Initialized Bach %s (Java %s, %s, %s)"
            .formatted(
                version(),
                System.getProperty("java.version"),
                System.getProperty("os.name"),
                Path.of(System.getProperty("user.dir")).toUri()));
  }

  protected Logbook constructLogbook() {
    return new Logbook();
  }

  public Configuration configuration() {
    return configuration;
  }

  public Logbook logbook() {
    return logbook;
  }

  @Override
  public void close() {
    writeLogbook();
    log("Total uptime was %s".formatted(DurationSupport.toHumanReadableString(logbook().uptime())));
  }

  public PrintWriter out() {
    return configuration().printing().out();
  }

  public PrintWriter err() {
    return configuration().printing().out();
  }

  protected Optional<String> computeRunMessageLine(ToolProvider provider, List<String> arguments) {
    var name = provider.name();
    var args = String.join(" ", arguments);
    var line = "%16s %s".formatted(name, args).stripTrailing();
    return Optional.of(line.length() <= 111 ? line : line.substring(0, 111 - 3) + "...");
  }

  public void log(String text) {
    if (text.toLowerCase(Locale.ROOT).startsWith("caption:")) {
      log(caption(text.substring("caption:".length())));
      return;
    }
    log(message(text));
  }

  public void log(Note note) {
    logbook.add(note);

    if (note instanceof Logbook.CaptionNote caption) {
      print(caption);
      return;
    }
    if (note instanceof Logbook.MessageNote message) {
      print(message);
      return;
    }
    if (note instanceof Logbook.RunNote run) {
      print(run);
      return;
    }
    // default -> { ... }
    out().println(note);
  }

  protected void print(Logbook.CaptionNote note) {
    out().println();
    out().println(note.line());
  }

  protected void print(Logbook.MessageNote note) {
    var severity = note.level().getSeverity();
    var text = note.text();
    if (severity >= Level.ERROR.getSeverity()) {
      err().println(text);
      return;
    }
    if (severity >= Level.WARNING.getSeverity()) {
      out().println(text);
      return;
    }
    if (severity >= Level.INFO.getSeverity() || configuration().verbose()) {
      out().println(text);
    }
  }

  protected void print(Logbook.RunNote note) {
    var run = note.run();
    var printer = run.isError() ? err() : out();
    if (run.isError() || configuration().verbose()) {
      var output = run.output();
      var errors = run.errors();
      if (!output.isEmpty()) printer.println(output.indent(4).stripTrailing());
      if (!errors.isEmpty()) printer.println(errors.indent(4).stripTrailing());
      printer.printf(
          "Tool '%s' run with %d argument%s took %s and finished with exit code %d%n",
          run.name(),
          run.args().size(),
          run.args().size() == 1 ? "" : "s",
          DurationSupport.toHumanReadableString(run.duration()),
          run.code());
    }
  }

  public Run run(Call call) {
    if (call instanceof Call.ToolCall tool) {
      var finder = tool.finder().orElse(configuration().tooling().finder());
      var provider = finder.find(tool.name());
      if (provider.isEmpty())
        throw new RuntimeException("Tool '%s' not found".formatted(tool.name()));
      return run(provider.get(), tool.arguments());
    }
    if (call instanceof Call.ProcessCall process) {
      var tool = new ExecuteProcessToolProvider();
      var command =
          Stream.concat(Stream.of(process.executable().toString()), process.arguments().stream())
              .toList();
      return run(tool, command);
    }
    if (call instanceof Call.ModuleCall module) {
      var tool = new ExecuteModuleToolProvider(module.finder());
      var arguments =
          Stream.concat(Stream.of(module.module()), module.arguments().stream()).toList();
      return run(tool, arguments);
    }
    throw new AssertionError("Where art thou, switch o' patterns?");
  }

  public Run run(String tool, Object... args) {
    return run(Call.tool(tool, args));
  }

  public Run run(ToolFinder finder, String name, Object... args) {
    return run(Call.tool(finder, name, args));
  }

  public Run run(Path executable, Object... args) {
    return run(Call.process(executable, args));
  }

  public Run run(ModuleFinder finder, String module, Object... args) {
    return run(Call.module(finder, module, args));
  }

  public Run run(ToolProvider provider, Object... args) {
    return run(provider, Call.tool(provider.name(), args).arguments());
  }

  public Run run(ToolProvider provider, List<String> arguments) {
    computeRunMessageLine(provider, arguments).ifPresent(this::log);

    var currentThread = Thread.currentThread();
    var currentLoader = currentThread.getContextClassLoader();
    currentThread.setContextClassLoader(provider.getClass().getClassLoader());

    var out = new StringWriter();
    var err = new StringWriter();
    var start = Instant.now();
    int code;
    try {
      var strings = arguments.toArray(String[]::new);
      code = provider.run(new PrintWriter(out), new PrintWriter(err), strings);
    } finally {
      currentThread.setContextClassLoader(currentLoader);
    }

    var name = provider.name();
    var duration = Duration.between(start, Instant.now());
    var thread = currentThread.getId();
    var output = out.toString().strip();
    var errors = err.toString().strip();

    var run = new Run(name, arguments, thread, duration, code, output, errors);
    var description = ToolProviderSupport.describe(provider);
    log(new Logbook.RunNote(run, description));

    return configuration().lenient() ? run : run.requireSuccessful();
  }

  public void run(Stream<Call> calls) {
    calls.forEach(this::run);
  }

  public void run(Call... calls) {
    run(Stream.of(calls));
  }

  public void runParallel(Call... calls) {
    run(Stream.of(calls).parallel());
  }

  public void writeLogbook() {
    try {
      var file = logbook().write();
      log("caption:Wrote logbook to %s".formatted(file.toUri()));
    } catch (Exception exception) {
      exception.printStackTrace(err());
    }
  }
}
