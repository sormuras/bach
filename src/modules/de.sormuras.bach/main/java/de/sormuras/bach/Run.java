package de.sormuras.bach;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.WARNING;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.spi.ToolProvider;

/*BODY*/
/** Runtime context information. */
public /*STATIC*/ class Run {

  /** Create default Run instance. */
  public static Run system() {
    var threshold = System.getProperties().containsKey("Debug".substring(1)) ? DEBUG : INFO;
    var dry = System.getProperties().containsKey("Dry-run".substring(1));
    var out = new PrintWriter(System.out, true, UTF_8);
    var err = new PrintWriter(System.err, true, UTF_8);
    return new Run(threshold, dry, out, err);
  }

  /** Current logging level threshold. */
  final System.Logger.Level threshold;
  /** Dry-run flag. */
  final boolean dryRun;
  /** Stream to which normal and expected output should be written. */
  final PrintWriter out;
  /** Stream to which any error messages should be written. */
  final PrintWriter err;
  /** Time instant recorded on creation of this instance. */
  final Instant start;

  Run(System.Logger.Level threshold, boolean dryRun, PrintWriter out, PrintWriter err) {
    this.start = Instant.now();
    this.dryRun = dryRun;
    this.threshold = threshold;
    this.out = out;
    this.err = err;
  }

  /** Log debug message unless threshold suppresses it. */
  void info(String format, Object... args) {
    log(INFO, format, args);
  }

  /** Log debug message unless threshold suppresses it. */
  void log(String format, Object... args) {
    log(DEBUG, format, args);
  }

  /** Log message unless threshold suppresses it. */
  void log(System.Logger.Level level, String format, Object... args) {
    if (level.getSeverity() < threshold.getSeverity()) {
      return;
    }
    var consumer = level.getSeverity() < WARNING.getSeverity() ? out : err;
    var message = String.format(format, args);
    consumer.println(message);
  }

  /** Run given command. */
  void run(Command command) {
    run(command.name, command.toStringArray());
  }

  /** Run named tool, as loaded by {@link java.util.ServiceLoader} using the system class loader. */
  void run(String name, String... args) {
    run(ToolProvider.findFirst(name).orElseThrow(), args);
  }

  /** Run provided tool. */
  void run(ToolProvider tool, String... args) {
    log("Running tool '%s' with: %s", tool.name(), List.of(args));
    var code = tool.run(out, err, args);
    if (code == 0) {
      log("Tool '%s' successfully run.", tool.name());
      return;
    }
    throw new Error("Tool '" + tool.name() + "' run failed with error code: " + code);
  }

  long toDurationMillis() {
    return TimeUnit.MILLISECONDS.convert(Duration.between(start, Instant.now()));
  }

  @Override
  public String toString() {
    return String.format(
        "Run{dryRun=%s, threshold=%s, start=%s, out=%s, err=%s}",
        dryRun, threshold, start, out, err);
  }
}
