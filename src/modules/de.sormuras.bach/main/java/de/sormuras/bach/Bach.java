/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

/*BODY*/
public class Bach {

  public static String VERSION = "1.9-ea";

  /**
   * Main entry-point.
   *
   * @param args List of API method or tool names.
   */
  public static void main(String... args) {
    var bach = new Bach();
    try {
      bach.main(args.length == 0 ? List.of("build") : List.of(args));
    } catch (Throwable throwable) {
      bach.err.printf("Bach.java (%s) failed: %s%n", VERSION, throwable.getMessage());
      if (bach.verbose) {
        throwable.printStackTrace(bach.err);
      } else {
        var causes = new ArrayDeque<>();
        var cause = throwable;
        while (cause != null && !causes.contains(cause)) {
          causes.add(cause);
          cause = cause.getCause();
        }
        bach.err.println(causes.getLast());
      }
    }
  }

  /** Text-output writer. */
  /*PRIVATE*/ final PrintWriter out, err;
  /** Be verbose. */
  private final boolean verbose;
  /** Project to be built. */
  /*PRIVATE*/ final Project project;
  /** Summary. */
  final Summary summary;

  /** Initialize default instance. */
  public Bach() {
    this(Log.ofSystem(), Project.of(Path.of("")));
  }

  /** Initialize. */
  public Bach(Log log, Project project) {
    this(log.out, log.err, log.verbose, project);
  }

  /** Initialize. */
  public Bach(PrintWriter out, PrintWriter err, boolean verbose, Project project) {
    this.out = Util.requireNonNull(out, "out");
    this.err = Util.requireNonNull(err, "err");
    this.verbose = verbose;
    this.project = project;
    this.summary = new Summary();
    log("New Bach.java (%s) instance initialized: %s", VERSION, this);
  }

  /** Print "debug" message to the standard output stream. */
  void log(String format, Object... args) {
    if (verbose) out.println(String.format(format, args));
  }

  /** Print "warning" message to the error output stream. */
  void warn(String format, Object... args) {
    err.println(String.format(format, args));
  }

  /** Non-static entry-point used by {@link #main(String...)} and {@code BachToolProvider}. */
  void main(List<String> arguments) {
    var tasks = Util.requireNonEmpty(Task.of(this, arguments), "tasks");
    log("Running %d argument task(s): %s", tasks.size(), tasks);
    tasks.forEach(consumer -> consumer.accept(this));
  }

  /** Run the passed command. */
  void run(Command command) {
    var name = command.getName();
    var tool = ToolProvider.findFirst(name);
    int code = run(tool.orElseThrow(() -> new RuntimeException(name)), command.toStringArray());
    if (code != 0) {
      throw new AssertionError("Running command failed: " + command);
    }
  }

  /** Run the tool using the passed provider and arguments. */
  int run(ToolProvider tool, String... arguments) {
    var line = tool.name() + (arguments.length == 0 ? "" : ' ' + String.join(" ", arguments));
    log("Running %s", line);
    var start = Instant.now();
    int code = tool.run(out, err, arguments);
    var duration = Duration.between(start, Instant.now());
    summary.runs.add(String.format("%d %6s ms %s", code, duration.toMillis(), line));
    return code;
  }

  /** Get the {@code Bach.java} banner. */
  private String banner() {
    var module = getClass().getModule();
    try (var stream = module.getResourceAsStream("de/sormuras/bach/banner.txt")) {
      if (stream == null) {
        return String.format("Bach.java %s (member of %s)", VERSION, module);
      }
      var lines = new BufferedReader(new InputStreamReader(stream)).lines();
      var banner = lines.collect(Collectors.joining(System.lineSeparator()));
      return banner + " " + VERSION;
    } catch (IOException e) {
      throw new UncheckedIOException("Loading banner resource failed", e);
    }
  }

  /** Verbosity flag. */
  boolean verbose() {
    return verbose;
  }

  /** Print help text to the standard output stream. */
  public void help() {
    out.println(banner());
    out.println("Method API");
    Arrays.stream(getClass().getMethods())
        .filter(Util::isApiMethod)
        .map(m -> "  " + m.getName() + " (" + m.getDeclaringClass().getSimpleName() + ")")
        .sorted()
        .forEach(out::println);
    out.println("Provided tools");
    ServiceLoader.load(ToolProvider.class).stream()
        .map(provider -> "  " + provider.get().name())
        .sorted()
        .forEach(out::println);
  }

  /** Build. */
  public void build() throws Exception {
    info();

    resolve();

    var units = project.realms.stream().map(realm -> realm.units).mapToLong(Collection::size).sum();
    if (units == 0) {
      throw new AssertionError("No units declared: " + project.realms);
    }

    // compile := javac + jar
    var realms = new ArrayDeque<>(project.realms);
    var main = realms.removeFirst();
    compile(main);
    for (var remaining : realms) {
      compile(remaining);
    }

    // test
    for (var remaining : realms) {
      new Tester(this, remaining).test();
    }
    System.gc(); // free file handles, e.g. modules loaded by custom module layer
    Util.sleep(Util.isWindows() ? 2345 : 1);

    // document := javadoc + deploy
    if (!main.units.isEmpty()) {
      var scribe = new Scribe(this, project, main);
      scribe.document();
      scribe.generateMavenInstallScript();
      if (main.toolArguments.deployment().isPresent()) {
        scribe.generateMavenDeployScript();
      }
    }

    summary(main);
  }

  private void compile(Project.Realm realm) {
    if (realm.units.isEmpty()) {
      return;
    }
    var hydras = realm.names(true);
    if (!hydras.isEmpty()) {
      new Hydra(this, project, realm).compile(hydras);
    }
    var jigsaws = realm.names(false);
    if (!jigsaws.isEmpty()) {
      new Jigsaw(this, project, realm).compile(jigsaws);
    }
  }

  /** Print summary. */
  public void summary(Project.Realm realm) throws Exception {
    out.println();
    out.printf("+---%n");
    out.printf("| Summary for project %s %s%n", project.name, project.version);
    out.printf("+---%n");
    out.println();
    var target = project.target(realm);
    var modulePath = project.modulePaths(target);
    var names = String.join(",", realm.names());
    var deps = new Command("jdeps").add("--module-path", modulePath).add("--multi-release", "BASE");
    run(
        deps.clone()
            .add("-summary")
            .add("--dot-output", target.directory.resolve("jdeps"))
            .add("--add-modules", names));
    if (verbose) {
      run(deps.clone().add("--check", names));
    }
    out.printf("Commands%n");
    var summaryBatch = ("summary-commands-" + Instant.now() + ".bat").replace(':', '-');
    Files.write(project.targetDirectory.resolve(summaryBatch), summary.runs);
    summary.runs.forEach(line -> out.printf("%.120s%s%n", line, line.length() <= 120 ? "" : "..."));
    out.println();
    out.printf("Modules%n");
    var jars = Util.list(target.modules, Util::isJarFile);
    out.printf("%d jar(s) found in: %s%n", jars.size(), target.modules.toUri());
    for (var jar : jars) {
      out.printf("%,11d %s%n", Files.size(jar), jar.getFileName());
    }
  }

  /** Print all "interesting" information. */
  public void info() {
    out.printf("Bach.java (%s)%n", VERSION);
    out.printf("+---%n");
    out.printf("| Project %s %s%n", project.name, project.version);
    out.printf("+---%n");
    try {
      for (var field : project.getClass().getFields()) {
        out.printf("  %s = %s%n", field.getName(), field.get(project));
      }
      for (var realm : project.realms) {
        out.printf("+ Realm %s%n", realm.name);
        for (var field : realm.getClass().getFields()) {
          out.printf("  %s.%s = %s%n", realm.name, field.getName(), field.get(realm));
        }
        for (var unit : realm.units) {
          out.printf("- ModuleUnit %s%n", unit.name());
          for (var field : unit.getClass().getFields()) {
            out.printf("  (%s).%s = %s%n", unit.name(), field.getName(), field.get(unit));
          }
        }
      }
    } catch (ReflectiveOperationException e) {
      e.printStackTrace(err);
    }
  }

  /** Resolve missing modules. */
  public void resolve() throws Exception {
    new Resolver(this).resolve();
  }

  /** Print Bach.java's version to the standard output stream. */
  public void version() {
    out.println(VERSION);
  }

  private static class Summary {
    final List<String> runs = new ArrayList<>();
  }
}
