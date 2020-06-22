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
import de.sormuras.bach.internal.Paths;
import de.sormuras.bach.project.Project;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;

/** A builder builds the project assigned to the given bach instance. */
public class Builder {

  private final Bach bach;
  private final Logbook logbook;
  private final Project project;

  public Builder(Bach bach) {
    this.bach = bach;
    this.logbook = bach.logbook();
    this.project = bach.project();
  }

  public void build() {
    var caption = "Build of " + project.toNameAndVersion();
    var projectInfoJava = String.join(System.lineSeparator(), project.toStrings());
    logbook.print(Level.INFO, "%s started...", caption);
    logbook.print(Level.DEBUG, "\tflags = %s", bach.flags());
    logbook.print(Level.DEBUG, "\tlogbook.threshold = %s", logbook.threshold());
    logbook.print(Level.TRACE, "\tproject-info.java = ...\n%s", projectInfoJava);

    var start = Instant.now();

    var factory = Executors.defaultThreadFactory();
    try (var executor = Concurrency.shutdownOnClose(Executors.newFixedThreadPool(2, factory))) {
      executor.submit(this::compile);
      executor.submit(this::generateApiDocumentation);
    }

    printModuleStatistics(Level.INFO);

    var duration = Duration.between(start, Instant.now());
    logbook.print(Level.INFO, "%s took %d ms", caption, duration.toMillis());

    var markdown = logbook.toMarkdown(project);
    try {
      var logfile = project.structure().base().workspace("logbook.md");
      Files.createDirectories(logfile.getParent());
      Files.write(logfile, markdown);
      logbook.print(Level.INFO, "Logfile written to %s", logfile.toUri());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    var errors = logbook.errors();
    if (errors.isEmpty()) return;

    errors.forEach(error -> error.toStrings().forEach(System.err::println));
    var message = "Detected " + errors.size() + " error" + (errors.size() != 1 ? "s" : "");
    var failOnError = bach.isFailOnError();
    logbook.print(Level.WARNING, message + " -> fail-on-error: " + failOnError);
    if (failOnError) throw new AssertionError(message);
  }

  public Builder compile() throws Exception {
    var javac = project.main().javac();
    if (javac.activated()) bach.call(javac);

    var modules = project.structure().base().modules("");
    Paths.delete(modules);
    Files.createDirectories(modules);

    for (var unit : project.main().units().values()) {
      var jar = unit.jar();
      if (jar.activated()) bach.call(jar);
    }

    return this;
  }

  public void generateApiDocumentation() {
    var javadoc = project.main().javadoc();
    if (javadoc.activated()) bach.call(javadoc);
  }

  public void printModuleStatistics(Level level) {
    var directory = project.structure().base().modules("");
    var uri = directory.toUri().toString();
    var files = Paths.list(directory, Paths::isJarFile);
    logbook.print(level, "Directory %s contains", uri);
    for (var file : files) logbook.print(level, "%,12d %s", Paths.size(file), file.getFileName());
  }
}
