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

import de.sormuras.bach.action.CompileMainSpace;
import de.sormuras.bach.action.CompileTestSpace;
import de.sormuras.bach.action.CompileTestSpacePreview;
import de.sormuras.bach.action.DeleteClassesDirectories;
import de.sormuras.bach.action.ResolveMissingExternalModules;
import de.sormuras.bach.internal.Factory;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleDescriptor.Version;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Java Shell Builder - build modular projects with JDK tools.
 *
 * <p>As an example, a project named {@code demo} and with version {@code 47.11} can be built with
 * the following code:
 *
 * <pre>{@code
 * var configuration = Configuration.ofSystem();
 * var project = Project.of("demo", "47.11");
 * new Bach(configuration, project).build();
 * }</pre>
 *
 * <p>The Java Development Kit provides at least the following tools via the {@link
 * java.util.spi.ToolProvider ToolProvider} interface.
 *
 * <ul>
 *   <li>{@code jar} - create an archive for classes and resources, and manipulate or restore
 *       individual classes or resources from an archive
 *   <li>{@code javac} - read Java class and interface definitions and compile them into bytecode
 *       and class files
 *   <li>{@code javadoc} - generate HTML pages of API documentation from Java source files
 *   <li>{@code javap} - disassemble one or more class files
 *   <li>{@code jdeps} - launch the Java class dependency analyzer
 *   <li>{@code jlink} - assemble and optimize a set of modules and their dependencies into a custom
 *       runtime image
 *   <li>{@code jmod} - create JMOD files and list the content of existing JMOD files
 * </ul>
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/14/docs/specs/man">JDK Tools</a>
 */
public class Bach {

  /** Version of the Java Shell Builder. */
  public static final Version VERSION = Version.parse("11.7");

  /**
   * Main entry-point.
   *
   * @param args the arguments
   */
  public static void main(String... args) {
    Main.main(args);
  }

  /**
   * Create new Bach instance for the given project.
   *
   * @param project The project instance to use
   * @return A new {@link Bach} instance
   */
  @Factory
  public static Bach of(Project project) {
    var configuration = Configuration.ofSystem();
    return new Bach(configuration, project);
  }

  /**
   * Create new Bach instance with a project parsed from current user directory.
   *
   * @param operator The operator may return a modified project based on the parsed one
   * @see UnaryOperator#identity()
   * @return A new {@link Bach} instance
   */
  @Factory
  public static Bach of(UnaryOperator<Project> operator) {
    var project = Project.ofCurrentDirectory();
    return of(operator.apply(project));
  }

  private final Configuration configuration;
  private final Project project;
  private /*lazy*/ HttpClient http = null;

  public Bach(Configuration configuration, Project project) {
    this.configuration = configuration;
    this.project = project;
  }

  public final Configuration configuration() {
    return configuration;
  }

  public final Project project() {
    return project;
  }

  public final boolean is(Flag flag) {
    return configuration().flags().set().contains(flag);
  }

  public final boolean not(Flag flag) {
    return !is(flag);
  }

  public final HttpClient http() {
    if (http == null) http = newHttpClient();
    return http;
  }

  public void build() {
    build(Bach::executeDefaultBuildActions);
  }

  public void build(Consumer<Bach> strategy) {
    var logbook = configuration().logbook();
    var nameAndVersion = project().toNameAndVersion();
    logbook.log(Level.TRACE, toString());
    logbook.log(Level.INFO, "Build of project %s started by %s", nameAndVersion, this);
    logbook.log(Level.TRACE, "\tflags.set=%s", configuration().flags().set());
    logbook.log(Level.TRACE, "\tlogbook.threshold=%s", logbook.threshold());
    if (logbook.isOn(Level.DEBUG)) {
      logbook.print();
      logbook.print("Project Descriptor");
      project().toStrings().forEach(logbook::print);
    }
    var start = Instant.now();
    try {
      strategy.accept(this);
      logbook.printSummaryAndCheckErrors(this, System.err::println);
    } catch (Exception exception) {
      var message = logbook.log(Level.ERROR, "Build failed with throwing %s", exception);
      throw new AssertionError(message, exception);
    } finally {
      var file = logbook.write(this);
      var duration = Duration.between(start, Instant.now()).toMillis();
      logbook.print();
      logbook.print("Logbook written to %s", file.toUri());
      logbook.print("Build of project %s took %d ms", nameAndVersion, duration);
    }
  }

  public void executeDefaultBuildActions() {
    resolveMissingExternalModules();
    compileMainSpace();
    compileTestSpace();
    compileTestSpaceWithPreviewLanguageFeatures();
  }

  public void deleteClassesDirectories() {
    new DeleteClassesDirectories(this).execute();
  }

  public void resolveMissingExternalModules() {
    new ResolveMissingExternalModules(this).execute();
  }

  public void compileMainSpace() {
    new CompileMainSpace(this).execute();
  }

  public void compileTestSpace() {
    new CompileTestSpace(this).execute();
  }

  public void compileTestSpaceWithPreviewLanguageFeatures() {
    new CompileTestSpacePreview(this).execute();
  }

  public HttpClient newHttpClient() {
    return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
  }

  public void run(Call<?> toolCall) {
    var call = configuration().tweak().apply(toolCall);

    var logbook = configuration().logbook();
    logbook.log(Level.INFO, call.toDescriptiveLine());
    logbook.log(Level.DEBUG, call.toCommandLine());

    var provider = call.findProvider();
    if (provider.isEmpty()) {
      var message = logbook.log(Level.ERROR, "Tool provider with name '%s' not found", call.name());
      if (is(Flag.FAIL_FAST)) throw new AssertionError(message);
      return;
    }

    if (is(Flag.DRY_RUN)) return;

    var tool = provider.get();
    var currentThread = Thread.currentThread();
    var currentContextLoader = currentThread.getContextClassLoader();
    currentThread.setContextClassLoader(tool.getClass().getClassLoader());
    var out = new StringWriter();
    var err = new StringWriter();
    var args = call.toStringArray();
    var start = Instant.now();

    try {
      var code = tool.run(new PrintWriter(out), new PrintWriter(err), args);

      var duration = Duration.between(start, Instant.now());
      var normal = out.toString().strip();
      var errors = err.toString().strip();
      var result = logbook.add(call, normal, errors, duration, code);
      logbook.log(Level.DEBUG, "%s finished after %d ms", tool.name(), duration.toMillis());

      if (code == 0) return;

      var caption = logbook.log(Level.ERROR, "%s failed with exit code %d", tool.name(), code);
      var message = new StringJoiner(System.lineSeparator());
      message.add(caption);
      result.toStrings().forEach(message::add);
      if (is(Flag.FAIL_FAST)) throw new AssertionError(message);
    } catch (RuntimeException exception) {
      logbook.log(Level.ERROR, "%s failed throwing %s", tool.name(), exception);
      if (is(Flag.FAIL_FAST)) throw exception;
    } finally {
      currentThread.setContextClassLoader(currentContextLoader);
    }
  }

  public void run(Runnable... runnables) {
    run(Runnable::run, List.of(runnables));
  }

  public <E> void run(Consumer<E> consumer, Collection<E> collection) {
    run(consumer, Function.identity(), collection);
  }

  public <E, T> void run(Consumer<T> consumer, Function<E, T> mapper, Collection<E> collection) {
    collection.stream().parallel().map(mapper).forEach(consumer);
  }

  @Override
  public String toString() {
    return "Bach.java " + VERSION;
  }
}
