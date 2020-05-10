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

import java.util.ArrayList;
import java.util.List;
import java.util.spi.ToolProvider;

/** A tool call configuration. */
public interface Call {

  /** Return a short label of this tool call. */
  String toLabel();

  /** Return the tool provider running this tool. */
  ToolProvider toProvider();

  /** Return the arguments to pass to {@code ToolProvider#run(out, err, String...)}. */
  String[] toArguments();

  default Task toTask() {
    return new Task.RunTool(toLabel(), toProvider(), toArguments());
  }

  /** A mutable argument list builder. */
  class Arguments {
    private final List<String> list = new ArrayList<>();

    public Arguments add(Object argument) {
      list.add(argument.toString());
      return this;
    }

    public Arguments add(String key, Object value) {
      return add(key).add(value);
    }

    public Arguments add(String key, Object first, Object second) {
      return add(key).add(first).add(second);
    }

    public Arguments add(Arguments arguments) {
      list.addAll(arguments.list);
      return this;
    }

    public String[] toStringArray() {
      return list.toArray(String[]::new);
    }
  }

  /** A description of the frame or reason a tool is called. */
  final class Context {
    private final String realm;
    private final String module;

    public Context(String realm, String module) {
      this.realm = realm;
      this.module = module;
    }

    public String realm() {
      return realm;
    }

    public String module() {
      return module;
    }
  }

  /**
   * Tune arguments passed to tool calls.
   *
   * <p>Sample implementation:
   *
   * <pre><code>
   * if (tool instanceof Javac javac) javac.getArguments().add("--verbose");
   * </code></pre>
   */
  @FunctionalInterface
  interface Tuner {
    void tune(Call call, Context context);
  }
}