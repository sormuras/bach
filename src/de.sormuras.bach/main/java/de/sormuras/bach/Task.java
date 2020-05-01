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
import java.lang.System.Logger.Level;
import java.util.List;
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

  public Task() {
    this("", List.of());
  }

  public Task(String label, List<Task> list) {
    this.label = label.isBlank() ? getClass().getSimpleName() : label;
    this.list = list;
  }

  public String getLabel() {
    return label;
  }

  public List<Task> getList() {
    return list;
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
      var out = new StringWriter();
      var err = new StringWriter();
      bach.execute(tool, new PrintWriter(out), new PrintWriter(err), args);
      var outString = out.toString().strip();
      if (!outString.isEmpty()) bach.getLogger().log(Level.DEBUG, outString);
      var errString = err.toString().strip();
      if (!errString.isEmpty()) bach.getLogger().log(Level.WARNING, outString);
    }
  }
}
