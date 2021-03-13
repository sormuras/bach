package com.github.sormuras.bach;

import java.lang.System.Logger.Level;
import java.time.Duration;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/** A collector of text messages and tool runs. */
public final class Logbook {

  /**
   * A text message.
   *
   * @param level the severity level of the message
   * @param text the actual message
   */
  public record Message(Level level, String text) {}

  /**
   * A recording of a tool run.
   *
   * @param name the name of the tool
   * @param args the arguments of the tool run
   * @param thread the ID of the thread that ran the tool
   * @param duration the duration of the tool run
   * @param code the exit code of the tool run
   * @param output the normal and expected output of the tool run
   * @param errors the error message of the tool run
   */
  public record Run(
      String name,
      List<String> args,
      long thread,
      Duration duration,
      int code,
      String output,
      String errors) {

    /** {@return {@code true} if this recording represents an errored tool run} */
    public boolean isError() {
      return code != 0;
    }

    /** {@return {@code true} if this recording represents a successful tool run} */
    public boolean isSuccessful() {
      return code == 0;
    }

    /**
     * Returns silently if this recording represents a successful tool run.
     *
     * @throws RuntimeException if {@link #isError()} returns {@code true}
     */
    public void requireSuccessful() {
      if (isSuccessful()) return;
      throw new RuntimeException(name + " returned code " + code + "\n" + toString().indent(4));
    }
  }

  /** A multi-recordings collector. */
  public record Runs(List<Run> values) {

    /**
     * Returns silently if all recordings represent successful tool runs.
     *
     * @throws RuntimeException if any recording failed
     */
    public void requireSuccessful() {
      var errors = values.stream().filter(Run::isError).toList();
      if (errors.isEmpty()) return;
      if (errors.size() == 1) errors.get(0).requireSuccessful();
      throw new RuntimeException(errors.size() + " runs returned a non-zero code");
    }
  }

  private final Options options;
  private final boolean verbose;
  private final Queue<Message> messages;
  private final Queue<Run> runs;

  Logbook(Options options) {
    this.options = options;
    this.verbose = options.is(Options.Flag.VERBOSE);
    this.messages = new ConcurrentLinkedQueue<>();
    this.runs = new ConcurrentLinkedQueue<>();
  }

  public List<Message> messages() {
    return messages.stream().toList();
  }

  public List<Run> runs() {
    return runs.stream().toList();
  }

  public void log(Level level, String format, Object... args) {
    var text = args == null || args.length == 0 ? format : String.format(format, args);
    messages.add(new Message(level, text));
    if (level.getSeverity() >= Level.ERROR.getSeverity()) {
      options.err().println(text);
      return;
    }
    if (level.getSeverity() >= Level.WARNING.getSeverity()) {
      options.out().println(text);
      return;
    }
    if (verbose || level.getSeverity() >= Level.INFO.getSeverity()) options.out().println(text);
  }

  public void log(Run run) {
    runs.add(run);
  }
}
