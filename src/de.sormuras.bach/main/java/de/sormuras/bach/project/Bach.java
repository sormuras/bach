/*
 * Bach - Java Shell Builder
 * Copyright (C) 2020 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.sormuras.bach.project;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
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
import java.util.stream.Collectors;

/** Bach - Java Shell Builder. */
public class Bach {

  public static void main(String[] args) {
    var project =
        Project.of("air", "1068-BWV")
            .with(Locator.ofJitPack("se.jbee.inject", "jbee", "silk", "master-SNAPSHOT"))
            .with(
                Locator.ofJUnitPlatform("commons", "1.7.0-M1"),
                Locator.ofJUnitPlatform("console", "1.7.0-M1"),
                Locator.ofJUnitPlatform("engine", "1.7.0-M1"),
                Locator.ofJUnitPlatform("launcher", "1.7.0-M1"),
                Locator.ofJUnitPlatform("reporting", "1.7.0-M1"))
            .with(
                Locator.ofJUnitJupiter("", "5.7.0-M1"),
                Locator.ofJUnitJupiter("api", "5.7.0-M1"),
                Locator.ofJUnitJupiter("engine", "5.7.0-M1"),
                Locator.ofJUnitJupiter("params", "5.7.0-M1"))
            .with(
                Locator.ofCentral("junit", "junit", "junit", "4.13"),
                Locator.ofCentral("org.hamcrest", "org.hamcrest", "hamcrest", "2.2"),
                Locator.ofCentral(
                        "org.junit.vintage.engine",
                        "org.junit.vintage:junit-vintage-engine:5.7.0-M1")
                    .withVersion("5.7-M1")
                    .withSize(63969)
                    .withDigest("md5", "455be2fc44c7525e7f20099529aec037"));

    Bach.ofSystem().with(new Logbook(System.out::println, Level.ALL)).with(project).build();
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

  public Set<Flag> flags() {
    return flags;
  }

  public Logbook logbook() {
    return logbook;
  }

  public Project project() {
    return project;
  }

  public void build() {
    var caption = "Build of " + project().toNameAndVersion();
    var projectInfoJava = String.join("\n", project.toStrings());
    logbook().print(Level.INFO, "%s started...", caption);
    logbook().print(Level.DEBUG, "\tflags = %s", flags());
    logbook().print(Level.DEBUG, "\tlogbook.threshold = %s", logbook().directThreshold);
    logbook().print(Level.TRACE, "\tproject-info.java = ...\n%s", projectInfoJava);

    var start = Instant.now();

    call("javac", "--version");
    call("javadoc", "--version");
    call("jar", "--version");

    var duration = Duration.between(start, Instant.now());
    logbook().print(Level.INFO, "%s took %d ms", caption, duration.toMillis());

    var markdown = logbook().toMarkdown(project);
    try {
      var logfile = project().structure().paths().workspace("logbook.md");
      Files.write(logfile, markdown);
      logbook().print(Level.INFO, "Logfile written to %s", logfile.toUri());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    var errors = logbook().errors();
    if (errors.isEmpty()) return;

    errors.forEach(error -> error.toStrings().forEach(System.err::println));
    var message = "Detected " + errors.size() + " error" + (errors.size() != 1 ? "s" : "");
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
        return String.format("%-7s %6X| %s", level, thread, text);
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

      public boolean error() {
        return code != 0;
      }

      public String toDetailedCaption() {
        return tool + '-' + Integer.toHexString(System.identityHashCode(this));
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
      if (text.isEmpty()) return;
      var thread = Thread.currentThread().getId();
      var line = new Line(thread, level, text);
      lines.add(line);
      if (level.getSeverity() < directThreshold.getSeverity()) return;
      synchronized (lines) {
        var all = directThreshold == Level.ALL;
        directConsumer.accept(all ? line.toString() : text);
      }
    }

    public void called(Call call) {
      calls.add(call);
      print(Level.TRACE, call.out);
      print(Level.TRACE, call.err);
    }

    public List<Call> errors() {
      return calls.stream().filter(Call::error).collect(Collectors.toList());
    }

    public List<String> toMarkdown(Project project) {
      var md = new ArrayList<String>();
      md.add("# Logbook of " + project.toNameAndVersion());
      md.addAll(projectDescription(project));
      md.addAll(toolCallOverview());
      md.addAll(toolCallDetails());
      md.addAll(logbookLines());
      return md;
    }

    private List<String> projectDescription(Project project) {
      var md = new ArrayList<String>();
      md.add("");
      md.add("## Project");
      md.add("- name: " + project.basics().name());
      md.add("- version: " + project.basics().version());
      md.add("");
      md.add("### Project Descriptor");
      md.add("```text");
      md.addAll(project.toStrings());
      md.add("```");
      return md;
    }

    private List<String> toolCallOverview() {
      var md = new ArrayList<String>();
      md.add("");
      md.add("## Tool Call Overview");
      md.add("|    |Thread|Duration|Tool|Arguments");
      md.add("|----|-----:|-------:|----|---------");
      for (var call : calls) {
        var kind = ' ';
        var thread = call.thread;
        var millis = call.duration.toMillis();
        var tool = "[" + call.tool + "](#" + call.toDetailedCaption() + ")";
        var arguments = String.join(" ", call.args);
        var row = String.format("|%4c|%6X|%8d|%s|%s", kind, thread, millis, tool, arguments);
        md.add(row);
      }
      return md;
    }

    private List<String> toolCallDetails() {
      var md = new ArrayList<String>();
      md.add("");
      md.add("## Tool Call Details");
      for (var call : calls) {
        md.add("");
        md.add("### " + call.toDetailedCaption());
        md.add("- tool = `" + call.tool + '`');
        md.add("- args = `" + String.join(" ", call.args) + '`');
        if (!call.out.isEmpty()) {
          md.add("```text");
          md.add(call.out);
          md.add("```");
        }
        if (!call.err.isEmpty()) {
          md.add("```text");
          md.add(call.err);
          md.add("```");
        }
      }
      return md;
    }

    private List<String> logbookLines() {
      var md = new ArrayList<String>();
      md.add("");
      md.add("## All Lines");
      md.add("```");
      for (var line : lines) md.add(line.toString());
      md.add("```");
      return md;
    }
  }
}
