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

import de.sormuras.bach.project.Project;
import de.sormuras.bach.tool.Call;
import de.sormuras.bach.tool.ToolNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;

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

  public Set<Flag> flags() {
    return flags;
  }

  public Logbook logbook() {
    return logbook;
  }

  public Project project() {
    return project;
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

  public void info() {
    project().toStrings().forEach(logbook().consumer());
  }

  public void build() {
    new Builder(this).build();
  }

  void call(Call<?> call) {
    var provider = call.tool().orElseThrow(() -> newToolNotFoundException(call));
    var arguments = call.toStrings().toArray(String[]::new);

    var tool = provider.name();
    logbook().print(Level.INFO, (tool + ' ' + String.join(" ", arguments)).trim());

    if (isDryRun()) return;

    var thread = Thread.currentThread().getId();
    var out = new StringWriter();
    var err = new StringWriter();
    var start = Instant.now();

    var currentThread = Thread.currentThread();
    var currentContextLoader = currentThread.getContextClassLoader();
    try {
      currentThread.setContextClassLoader(provider.getClass().getClassLoader());
      var code = provider.run(new PrintWriter(out), new PrintWriter(err), arguments);

      var duration = Duration.between(start, Instant.now());
      var normal = out.toString().strip();
      var errors = err.toString().strip();
      var called = new Logbook.Called(thread, tool, arguments, normal, errors, duration, code);
      logbook().called(called);

      if (code == 0) return;

      var caption = String.format("%s failed with exit code %d", tool, code);
      logbook().print(Level.ERROR, caption);
      var message = new StringJoiner(System.lineSeparator());
      message.add(caption);
      called.toStrings().forEach(message::add);
      if (isFailFast()) throw new AssertionError(message);
    } catch (Throwable throwable) {
      var caption = String.format("%s failed throwing %s", tool, throwable);
      logbook().print(Level.ERROR, caption);
      if (isFailFast()) throw throwable;
    } finally {
      currentThread.setContextClassLoader(currentContextLoader);
    }
  }

  private ToolNotFoundException newToolNotFoundException(Call<?> call) {
    var exception = new ToolNotFoundException(call.name());
    logbook().print(Level.ERROR, exception.getMessage());
    return exception;
  }

  @Override
  public String toString() {
    return "Bach.java " + VERSION;
  }
}
