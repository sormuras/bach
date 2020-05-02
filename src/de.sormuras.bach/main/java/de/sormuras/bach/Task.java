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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.spi.ToolProvider;

/** A piece of work to be done or undertaken. */
public /*static*/ class Task {

  public static Task sequence(String label, Task... tasks) {
    return sequence(label, List.of(tasks));
  }

  public static Task sequence(String label, List<Task> tasks) {
    return new Task(label, tasks);
  }

  public static Task runTool(String name, Object... arguments) {
    var tool = ToolProvider.findFirst(name).orElseThrow();
    var args = new String[arguments.length];
    for (int i = 0; i < args.length; i++) args[i] = arguments[i].toString();
    return new RunTool(tool, args);
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

  static class RunTool extends Task {

    private final ToolProvider tool;
    private final String[] args;

    public RunTool(ToolProvider tool, String... args) {
      super(tool.name() + " " + String.join(" ", args), List.of());
      this.tool = tool;
      this.args = args;
    }

    @Override
    public void execute(Bach bach) {
      bach.execute(tool, new PrintWriter(getOut()), new PrintWriter(getErr()), args);
    }
  }

  static class CreateDirectories extends Task {

    final Path directory;

    CreateDirectories(Path directory) {
      super("Create directories " + directory.toUri(), List.of());
      this.directory = directory;
    }

    @Override
    public void execute(Bach bach) throws Exception {
      Files.createDirectories(directory);
    }
  }

  static class DeleteDirectories extends Task {

    final Path directory;

    DeleteDirectories(Path directory) {
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

  static class CreateJar extends Task {

    private static List<Task> list(Path jar, Path classes) {
      return List.of(
          new CreateDirectories(jar.getParent()),
          runTool("jar", "--create", "--file", jar, "-C", classes, "."),
          runTool("jar", "--describe-module", "--file", jar));
    }

    public CreateJar(Path jar, Path classes) {
      super("Create modular JAR file " + jar.getFileName(), list(jar, classes));
    }
  }
}
