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

import de.sormuras.bach.internal.Functions;
import de.sormuras.bach.internal.Logbook;
import java.io.PrintWriter;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleDescriptor.Version;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.spi.ToolProvider;

/** Bach - Java Shell Builder. */
public class Bach {

  /** Version of the Java Shell Builder. */
  public static final Version VERSION = Version.parse("11-ea");

  /** Default path of the custom build program source file. */
  public static final Path BUILD_JAVA = Path.of(".bach/src/Build.java");

  /** Workspace root directory to used for generated assets. */
  public static final Path WORKSPACE = Path.of(".bach/workspace");

  /** Main entry-point. */
  public static void main(String... args) {
    Main.main(args);
  }

  /** Return path to the custom build program if it exists. */
  public static Optional<Path> findCustomBuildProgram() {
    return Files.exists(BUILD_JAVA) ? Optional.of(BUILD_JAVA) : Optional.empty();
  }

  /** Return Bach instance with a default project scanned from the current working directory. */
  public static Bach of() {
    return of(UnaryOperator.identity());
  }

  /** Return Bach instance with a configured project scanned from the current working directory. */
  public static Bach of(UnaryOperator<Project.Builder> builder) {
    return of(UnaryOperator.identity(), builder);
  }

  /** Return Bach instance with both operators applied. */
  public static Bach of(UnaryOperator<Scanner> scanner, UnaryOperator<Project.Builder> builder) {
    return of(builder.apply(scanner.apply(new Scanner()).newBuilder()).newProject());
  }

  /** Return Bach instance with the specified project and default components. */
  public static Bach of(Project project) {
    var httpClientBuilder = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL);
    return new Bach(project, httpClientBuilder::build);
  }

  /** The logbook instance collecting all log entries. */
  private final Logbook logbook;

  /** The project to build. */
  private final Project project;

  /** The HttpClient supplier. */
  private final Supplier<HttpClient> httpClient;

  /** Initialize this instance with the specified project and other component values. */
  public Bach(Project project, Supplier<HttpClient> httpClient) {
    this(Logbook.ofSystem(), project, httpClient);
  }

  /** Canonical constructor. */
  /*private*/ Bach(Logbook logbook, Project project, Supplier<HttpClient> httpClient) {
    this.logbook = Objects.requireNonNull(logbook, "logbook");
    this.project = Objects.requireNonNull(project, "project");
    this.httpClient = Functions.memoize(httpClient);
    logbook.log(Level.TRACE, "Initialized " + toString());
    logbook.log(Level.DEBUG, project.toTitleAndVersion());
  }

  public Logger getLogger() {
    return logbook;
  }

  public Project getProject() {
    return project;
  }

  public HttpClient getHttpClient() {
    return httpClient.get();
  }

  public Summary build() {
    return build(Sequencer.Tuner::defaults);
  }

  public Summary build(Sequencer.Tuner tuner) {
    var summary = new Summary(this);
    try {
      execute(new Sequencer(project, tuner).newBuildSequence());
    } finally {
      summary.writeMarkdown(project.base().workspace("summary.md"), true);
    }
    return summary;
  }

  /*private*/ void execute(Task task) {
    var label = task.getLabel();
    var tasks = task.getList();
    if (tasks.isEmpty()) {
      logbook.log(Level.TRACE, "* {0}", label);
      if (logbook.isDryRun()) return;
      try {
        task.execute(this);
      } catch (Throwable throwable) {
        var message = "Task execution failed";
        logbook.log(Level.ERROR, message, throwable);
        throw new Error(message, throwable);
      } finally {
        logbook.log(Level.DEBUG, task.getOut().toString().strip());
        logbook.log(Level.INFO, task.getErr().toString().strip());
      }
      return;
    }
    logbook.log(Level.TRACE, "+ {0}", label);
    var start = System.currentTimeMillis();
    for (var sub : tasks) execute(sub);
    var duration = System.currentTimeMillis() - start;
    logbook.log(Level.TRACE, "= {0} took {1} ms", label, duration);
  }

  /*private*/ void execute(ToolProvider tool, PrintWriter out, PrintWriter err, String... args) {
    var call = (tool.name() + ' ' + String.join(" ", args)).trim();
    logbook.log(Level.DEBUG, call);
    var code = tool.run(out, err, args);
    if (code != 0) throw new AssertionError("Tool run exit code: " + code + "\n\t" + call);
  }

  @Override
  public String toString() {
    return "Bach.java " + VERSION;
  }
}
