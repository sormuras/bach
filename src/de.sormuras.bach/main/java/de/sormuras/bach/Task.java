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

import de.sormuras.bach.internal.Modules;
import de.sormuras.bach.internal.ModulesResolver;
import de.sormuras.bach.internal.Paths;
import de.sormuras.bach.internal.Resources;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.spi.ToolProvider;

/** A piece of work to be done or undertaken. */
public /*static*/ class Task {

  public static Task sequence(String label, Task... tasks) {
    return sequence(label, List.of(tasks));
  }

  public static Task sequence(String label, List<Task> tasks) {
    return new Task(label, tasks);
  }

  private final String label;
  private final List<Task> list;
  private final StringWriter out;
  private final StringWriter err;

  public Task() {
    this("", List.of());
  }

  public Task(String label, List<Task> list) {
    Objects.requireNonNull(label, "label");
    this.label = label.isBlank() ? getClass().getSimpleName() : label;
    this.list = List.copyOf(Objects.requireNonNull(list, "list"));
    this.out = new StringWriter();
    this.err = new StringWriter();
  }

  public String getLabel() {
    return label;
  }

  public List<Task> getList() {
    return list;
  }

  public StringWriter getOut() {
    return out;
  }

  public StringWriter getErr() {
    return err;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Task.class.getSimpleName() + "[", "]")
        .add("label='" + label + "'")
        .add("list.size=" + list.size())
        .toString();
  }

  public void execute(Bach bach) throws Exception {}

  public static class RunTool extends Task {

    private final ToolProvider tool;
    private final String[] args;

    public RunTool(String label, ToolProvider tool, String... args) {
      super(label, List.of());
      this.tool = tool;
      this.args = args;
    }

    @Override
    public void execute(Bach bach) {
      bach.execute(tool, new PrintWriter(getOut()), new PrintWriter(getErr()), args);
    }
  }

  public static class RunTestModule extends Task {

    private final String module;
    private final List<Path> modulePaths;

    public RunTestModule(String module, List<Path> modulePaths) {
      super("Run tests for module " + module, List.of());
      this.module = module;
      this.modulePaths = modulePaths;
    }

    @Override
    public void execute(Bach bach) {
      var currentThread = Thread.currentThread();
      var currentContextLoader = currentThread.getContextClassLoader();
      try {
        for (var tool : Modules.findTools(module, modulePaths)) executeTool(bach, tool);
      } finally {
        currentThread.setContextClassLoader(currentContextLoader);
      }
    }

    private void executeTool(Bach bach, ToolProvider tool) {
      Thread.currentThread().setContextClassLoader(tool.getClass().getClassLoader());
      if (tool.name().equals("test(" + module + ")")) {
        bach.execute(tool, new PrintWriter(getOut()), new PrintWriter(getErr()));
        return;
      }
      if (tool.name().equals("junit")) {
        var base = bach.getProject().base();
        bach.execute(
            tool,
            new PrintWriter(getOut()),
            new PrintWriter(getErr()),
            "--select-module",
            module,
            "--disable-ansi-colors",
            "--reports-dir",
            base.workspace("junit-reports", module).toString());
      }
    }
  }

  public static class CreateDirectories extends Task {

    private final Path directory;

    public CreateDirectories(Path directory) {
      super("Create directories " + directory.toUri(), List.of());
      this.directory = directory;
    }

    @Override
    public void execute(Bach bach) throws Exception {
      Files.createDirectories(directory);
    }
  }

  public static class DeleteDirectories extends Task {

    private final Path directory;

    public DeleteDirectories(Path directory) {
      super("Delete directory " + directory, List.of());
      this.directory = directory;
    }

    @Override
    public void execute(Bach bach) throws Exception {
      if (Files.notExists(directory)) return;
      try (var stream = Files.walk(directory)) {
        var paths = stream.sorted((p, q) -> -p.compareTo(q));
        for (var path : paths.toArray(Path[]::new)) Files.deleteIfExists(path);
      }
    }
  }

  public static class DeleteEmptyDirectory extends Task {

    private final Path directory;

    public DeleteEmptyDirectory(Path directory) {
      super("Delete directory " + directory + " if it is empty", List.of());
      this.directory = directory;
    }

    @Override
    public void execute(Bach bach) throws Exception {
      if (Files.notExists(directory)) return;
      if (Paths.isEmpty(directory)) Files.deleteIfExists(directory);
    }
  }

  /** Determine and transport missing 3<sup>rd</sup> party modules. */
  public static class ResolveMissingThirdPartyModules extends Task {

    public ResolveMissingThirdPartyModules() {
      super("Resolve missing 3rd-party modules", List.of());
    }

    @Override
    public void execute(Bach bach) {
      var project = bach.getProject();
      var library = project.structure().library();
      class Transporter implements Consumer<Set<String>> {
        @Override
        public void accept(Set<String> modules) {
          var resources = new Resources(bach.getHttpClient());
          for (var module : modules) {
            var raw = library.lookup().apply(module);
            if (raw == null) continue;
            try {
              var lib = Files.createDirectories(project.base().lib());
              var uri = URI.create(raw);
              var name = module + ".jar";
              var file = resources.copy(uri, lib.resolve(name));
              var size = Files.size(file);
              bach.getLogger().log(Level.INFO, "{0} ({1} bytes) << {2}", file, size, uri);
            } catch (Exception e) {
              throw new Error("Resolve module '" + module + "' failed: " + raw + "\n\t" + e, e);
            }
          }
        }
      }
      var modulePaths = project.base().modulePaths(List.of());
      var declared = project.toDeclaredModuleNames();
      var resolver = new ModulesResolver(modulePaths, declared, new Transporter());
      resolver.resolve(project.toRequiredModuleNames());
      resolver.resolve(library.required());
    }
  }
}
