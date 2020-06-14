package de.sormuras.bach.project;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleDescriptor.Version;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.spi.ToolProvider;

/** Bach - Java Shell Builder. */
public class Bach {

  public static void main(String[] args) {
    Bach.ofSystem()
        .without(Flag.FAIL_FAST)
        .without(Flag.FAIL_ON_ERROR)
        .with(new Logbook(System.out::println, Level.WARNING))
        .build();
  }

  public static Bach ofSystem() {
    var projectName = System.getProperty("project.name", "unnamed");
    var projectVersion = System.getProperty("project.name", "1-ea");
    var project = Project.of(projectName, projectVersion);

    return new Bach(Flag.ofSystem(), Logbook.ofSystem(), project);
  }

  /** Version of the Java Shell Builder. */
  public static final Version VERSION = Version.parse("11-ea");

  private final Set<Flag> flags;
  private final Logbook logbook;
  private final Project project;

  public Bach(Set<Flag> flags, Logbook logbook, Project project) {
    this.flags = flags.isEmpty() ? Set.of() : EnumSet.copyOf(flags);
    this.logbook = logbook;
    this.project = project;
  }

  public Bach with(Flag flag) {
    var flags = new TreeSet<>(this.flags);
    flags.add(flag);
    return with(flags);
  }

  public Bach without(Flag flag) {
    var flags = new TreeSet<>(this.flags);
    flags.remove(flag);
    return with(flags);
  }

  public Bach with(Set<Flag> flags) {
    return new Bach(flags, logbook, project);
  }

  public Bach with(Logbook logbook) {
    return new Bach(flags, logbook, project);
  }

  public Bach with(Project project) {
    return new Bach(flags, logbook, project);
  }

  public boolean isDryRun() {
    return flags.contains(Flag.DRY_RUN);
  }

  public boolean isFailFast() {
    return flags.contains(Flag.FAIL_FAST);
  }

  public boolean isFailOnError() {
    return flags.contains(Flag.FAIL_ON_ERROR);
  }

  public Logbook logbook() {
    return logbook;
  }

  public Project project() {
    return project;
  }

  public void build() {
    logbook().print(Level.INFO, "Build %s...", project().toNameAndVersion());

    logbook().print(Level.DEBUG, "bach.flags = %s", flags);

    call("javac", "--version");
    call("javac", "4711");
    call("javac", "--version");

    var errors = new ArrayList<Logbook.Call>();
    for (var call : logbook().calls) {
      System.out.println(call);
      if (call.code == 0) continue;
      errors.add(call);
      call.toStrings().forEach(System.out::println);
    }
    if (errors.isEmpty()) return;
    var message = "Build error(s) detected: " + errors.size();
    logbook().print(Level.WARNING, message + " -> fail-on-error: " + isFailOnError());
    if (isFailOnError()) throw new AssertionError(message);
  }

  public void call(String tool, Object... args) {
    var provider = ToolProvider.findFirst(tool).orElseThrow(() -> newToolNotFoundException(tool));
    var arguments = Arrays.stream(args).map(String::valueOf).toArray(String[]::new);
    call(provider, arguments);
  }

  void call(ToolProvider tool, String... args) {
    var name = tool.name();
    var command = (name + ' ' + String.join(" ", args)).trim();
    logbook().print(Level.INFO, command);

    if (isDryRun()) return;

    var thread = Thread.currentThread().getId();
    var out = new StringWriter();
    var err = new StringWriter();
    var start = Instant.now();

    var code = tool.run(new PrintWriter(out), new PrintWriter(err), args);

    var duration = Duration.between(start, Instant.now());
    var normal = out.toString().strip();
    var errors = err.toString().strip();
    var call = new Logbook.Call(thread, name, args, normal, errors, duration, code);
    logbook().called(call);

    if (code == 0) return;

    var caption = String.format("%s failed with exit code %d", name, code);
    logbook().print(Level.ERROR, caption);
    var message = new StringJoiner(System.lineSeparator());
    message.add(caption);
    call.toStrings().forEach(message::add);
    if (isFailFast()) throw new AssertionError(message);
  }

  private IllegalStateException newToolNotFoundException(String name) {
    var message = "Tool with name \"" + name + "\" not found";
    logbook().print(Level.ERROR, message);
    return new IllegalStateException(message);
  }

  public enum Flag {
    DRY_RUN(false),
    FAIL_FAST(true),
    FAIL_ON_ERROR(true);

    private final boolean enabledByDefault;

    Flag(boolean enabledByDefault) {
      this.enabledByDefault = enabledByDefault;
    }

    public boolean isEnabledByDefault() {
      return enabledByDefault;
    }

    public String key() {
      return name().toLowerCase().replace('_', '-');
    }

    public static Set<Flag> ofSystem() {
      var flags = new TreeSet<Flag>();
      for (var flag : values()) {
        var property = System.getProperty(flag.key(), flag.isEnabledByDefault() ? "true" : "false");
        if (Boolean.parseBoolean(property)) flags.add(flag);
      }
      return EnumSet.copyOf(flags);
    }
  }

  public static class Logbook {

    static final class Line {
      private final long thread;
      private final Level level;
      private final String text;

      Line(long thread, Level level, String text) {
        this.thread = thread;
        this.level = level;
        this.text = text;
      }

      @Override
      public String toString() {
        return new StringJoiner(", ", Line.class.getSimpleName() + "[", "]")
            .add("thread=" + thread)
            .add("level=" + level)
            .add("text='" + text + "'")
            .toString();
      }
    }

    static final class Call {
      private final long thread;
      private final String tool;
      private final String[] args;
      private final String out;
      private final String err;
      private final Duration duration;
      private final int code;

      Call(
          long thread,
          String tool,
          String[] args,
          String normal,
          String errors,
          Duration duration,
          int code) {
        this.thread = thread;
        this.tool = tool;
        this.args = args;
        this.out = normal;
        this.err = errors;
        this.duration = duration;
        this.code = code;
      }

      public List<String> toStrings() {
        var message = new ArrayList<String>();
        message.add("");
        message.add('\t' + tool + ' ' + String.join(" ", args));
        if (!out.isEmpty()) {
          message.add("");
          out.lines().forEach(line -> message.add("\t\t" + line));
        }
        if (!err.isEmpty()) {
          message.add("");
          err.lines().forEach(line -> message.add("\t\t" + line));
        }
        message.add("");
        return message;
      }

      @Override
      public String toString() {
        return new StringJoiner(", ", Call.class.getSimpleName() + "[", "]")
            .add("thread=" + thread)
            .add("tool='" + tool + "'")
            .add("args=" + Arrays.toString(args))
            .add("out=[" + out.length() + " chars]")
            .add("err=[" + err.length() + " chars]")
            .add("time=" + duration)
            .add("code=" + code)
            .toString();
      }
    }

    public static Logbook ofSystem() {
      var logbookThreshold = System.getProperty("logbook.threshold", "INFO");
      return new Logbook(System.out::println, Level.valueOf(logbookThreshold));
    }

    private final Queue<Line> lines = new ConcurrentLinkedQueue<>();
    private final Queue<Call> calls = new ConcurrentLinkedQueue<>();
    private final Consumer<String> directConsumer;
    private final Level directThreshold;

    public Logbook(Consumer<String> directConsumer, Level directThreshold) {
      this.directConsumer = directConsumer;
      this.directThreshold = directThreshold;
    }

    public Logbook with(Consumer<String> consumer) {
      return new Logbook(consumer, directThreshold);
    }

    public Logbook with(Level threshold) {
      return new Logbook(directConsumer, threshold);
    }

    public void print(Level level, String format, Object... arguments) {
      print(level, String.format(format, arguments));
    }

    public void print(Level level, String text) {
      var thread = Thread.currentThread().getId();
      lines.add(new Line(thread, level, text));
      if (level.getSeverity() < directThreshold.getSeverity()) return;
      synchronized (lines) {
        var all = directThreshold == Level.ALL;
        directConsumer.accept(all ? String.format("%-7s %6X| %s", level, thread, text) : text);
      }
    }

    public void called(Call call) {
      calls.add(call);
    }
  }
}
