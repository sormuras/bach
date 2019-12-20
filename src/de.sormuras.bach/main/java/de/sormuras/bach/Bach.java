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

import de.sormuras.bach.project.Folder;
import de.sormuras.bach.project.Project;
import de.sormuras.bach.project.ProjectBuilder;
import de.sormuras.bach.task.StartTask;
import de.sormuras.bach.util.Modules;
import de.sormuras.bach.util.Paths;
import de.sormuras.bach.util.Tools;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;
import java.util.spi.ToolProvider;

/** Modular Java project builder. */
public class Bach {

  /** Default values. */
  public /*sealed*/ interface Default /*permits empty-set*/ {
    Path BASE = Path.of("");
    Path SRC = Path.of("src");
    Path LIB = Path.of("lib");
    Path OUT = Path.of(".bach/out");
  }

  /** Main entry-point. */
  public static void main(String... args) {
    var log = Log.ofSystem();
    var folder = Folder.of(Path.of(""));
    if (args.length == 0) {
      build(log, new ProjectBuilder(log).auto(folder));
      return;
    }
    var arguments = new ArrayDeque<>(List.of(args));
    var argument = arguments.pop();
    switch (argument) {
      case "clean":
        Paths.deleteIfExists(folder.out());
        return;
      case "build":
        build(log, new ProjectBuilder(log).auto(folder));
        return;
      case "help":
        System.out.println("F1 F1 F1");
        return;
      case "start":
        new Bach(log, new ProjectBuilder(log).auto(folder)).execute(new StartTask(arguments));
        return;
      default:
        throw new Error("Unsupported argument: " + argument + " // args = " + List.of(args));
    }
  }

  /** Build entry-point. */
  public static void build(Log log, Project project) {
    var bach = new Bach(log, project);
    bach.execute(Task.build());
  }

  private final Log log;
  private final Project project;
  private final Tools tools;

  public Bach(Log log, Project project) {
    this.log = log;
    this.project = project;
    this.tools = new Tools();
    log.debug("Bach.java (%s) initialized.", Modules.origin(this));
    logRuntimeAndProjectInformation();
  }

  private void logRuntimeAndProjectInformation() {
    log.debug("Runtime information");
    log.debug("  - java.version = " + System.getProperty("java.version"));
    log.debug("  - user.dir = " + System.getProperty("user.dir"));
    log.debug("Tools of the trade");
    tools.forEach(t -> log.debug("  - %8s [%s] %s", t.name(), Modules.origin(t), t));
    log.info("Project %s %s", project.name(), project.version());
    try {
      for (var field : project.getClass().getFields()) {
        log.debug("  %s = %s", field.getName(), field.get(project));
      }
      for (var realm : project.structure().realms()) {
        log.debug("+ Realm %s", realm.name());
        for (var method : realm.getClass().getDeclaredMethods()) {
          if (method.getParameterCount() != 0) continue;
          log.debug("  %s.%s() = %s", realm.name(), method.getName(), method.invoke(realm));
        }
        for (var unit : project.structure().units()) {
          log.debug("- Unit %s", unit.name());
          for (var method : unit.getClass().getDeclaredMethods()) {
            if (method.getParameterCount() != 0) continue;
            log.debug("  (%s).%s() = %s", unit.name(), method.getName(), method.invoke(unit));
          }
        }
      }
    } catch (ReflectiveOperationException e) {
      log.warning(e.getMessage());
    }
  }

  public Log getLog() {
    return log;
  }

  public Project getProject() {
    return project;
  }

  public boolean isVerbose() {
    return log.verbose;
  }

  public Bach execute(Task... tasks) {
    try {
      for (var task : tasks) {
        var name = task.getClass().getSimpleName();
        try (var context = log.context(task)) {
          var entry = log.debug("Executing task: %s", name);
          context.task().execute(this);
          var duration = Duration.between(entry.instant(), Instant.now()).toMillis();
          log.debug("%s took %d millis.", name, duration);
        }
      }
    } catch (Exception e) {
      throw new Error("Task failed to execute: " + e, e);
    }
    return this;
  }

  /** Execute {@link Call} composed of given name and arguments converted to an array of strings. */
  public Bach execute(String name, Object... arguments) {
    var strings = new String[arguments.length];
    for (int i = 0; i < arguments.length; i++) strings[i] = arguments[i].toString();
    return execute(new Call(name, strings));
  }

  /** Execute all given calls, fail-fast style. */
  public Bach execute(Call... calls) {
    for (var call : calls) {
      var code = run(call);
      if (code != 0) {
        throw new Error("Call exited with non-zero exit code: " + code + " <- " + call);
      }
    }
    return this;
  }

  public int run(Call call) {
    return run(tools.get(call.name), call);
  }

  public int run(ToolProvider tool, Call call) {
    var name = tool.name();
    var args = call.toArray(false);
    var entry = log.debug("Running tool: %s %s", name, String.join(" ", args));
    var code = tool.run(log.out, log.err, args);
    log.tool(name, args, Duration.between(entry.instant(), Instant.now()), code);
    return code;
  }

  public int start(List<String> command) throws IOException, InterruptedException {
    var name = command.get(0);
    var args = command.subList(1, command.size()).toArray(String[]::new);
    var entry = log.debug("Starting command: %s", String.join(" ", command));
    var code = new ProcessBuilder(command).inheritIO().start().waitFor();
    log.tool(name, args, Duration.between(entry.instant(), Instant.now()), code);
    return code;
  }
}
