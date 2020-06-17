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

package de.sormuras.bach;

import de.sormuras.bach.internal.Concurrency;
import de.sormuras.bach.project.Project;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.spi.ToolProvider;

/** Bach - Java Shell Builder. */
public class Bach {

  /** Version of the Java Shell Builder. */
  public static final Version VERSION = Version.parse("11-ea");

  /** Default path of a custom build program source file. */
  public static final Path BUILD_JAVA = Path.of(".bach/src/build/build/Build.java");

  /**
   * Main entry-point.
   *
   * @param args the arguments
   */
  public static void main(String... args) {
    Main.main(args);
  }

  public static Bach ofSystem() {
    var projectName = System.getProperty("project.name", "unnamed");
    var projectVersion = System.getProperty("project.name", "1-ea");
    var project = Project.of(projectName, projectVersion);

    return of(project);
  }

  public static Bach of(Project project) {
    return new Bach(Flag.ofSystem(), Logbook.ofSystem(), project);
  }

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

  public void info() {
    project().toStrings().forEach(logbook().consumer());
  }

  public void build() {
    var caption = "Build of " + project().toNameAndVersion();
    var projectInfoJava = String.join(System.lineSeparator(), project.toStrings());
    logbook().print(Level.INFO, "%s started...", caption);
    logbook().print(Level.DEBUG, "\tflags = %s", flags());
    logbook().print(Level.DEBUG, "\tlogbook.threshold = %s", logbook().threshold());
    logbook().print(Level.TRACE, "\tproject-info.java = ...\n%s", projectInfoJava);

    var start = Instant.now();

    var factory = Executors.defaultThreadFactory();
    try (var executor = Concurrency.shutdownOnClose(Executors.newFixedThreadPool(2, factory))) {
      executor.submit(this::compile);
      executor.submit(this::generateApiDocumentation);
    }

    var duration = Duration.between(start, Instant.now());
    logbook().print(Level.INFO, "%s took %d ms", caption, duration.toMillis());

    var markdown = logbook().toMarkdown(project);
    try {
      var logfile = project().structure().base().workspace("logbook.md");
      Files.createDirectories(logfile.getParent());
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

  public void compile() {
    call("javac", "--version");
    project().main().unitNames().forEach(name -> call("jar", "--version"));
  }

  public void generateApiDocumentation() {
    call("javadoc", "--version");
  }

  private IllegalStateException newToolNotFoundException(String name) {
    var message = "Tool with name \"" + name + "\" not found";
    logbook().print(Level.ERROR, message);
    return new IllegalStateException(message);
  }

  @Override
  public String toString() {
    return "Bach.java " + VERSION;
  }
}
