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

import de.sormuras.bach.project.Project;
import de.sormuras.bach.project.ProjectBuilder;
import de.sormuras.bach.util.Tools;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.spi.ToolProvider;

/** Build modular Java project. */
public class Bach {

  /** Main entry-point. */
  public static void main(String... args) {
    var log = Log.ofSystem();
    var base = Path.of(".").normalize();
    var project = new ProjectBuilder(log).auto(base);
    build(log, project);
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
    var nameAndVersion =
        Optional.ofNullable(getClass().getModule().getDescriptor())
            .map(ModuleDescriptor::toNameAndVersion)
            .orElse("UNNAMED MODULE");
    log.debug("Bach.java (%s) initialized.", nameAndVersion);
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

  public void execute(Task... tasks) {
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
  }

  public void execute(Call... calls) {
    for (var call : calls) {
      var code = run(call);
      if (code != 0) {
        throw new Error("Call exited with non-zero exit code: " + code + " <- " + call);
      }
    }
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
}
