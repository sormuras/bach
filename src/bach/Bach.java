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

// default package

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.spi.ToolProvider;

/** Java Shell Builder. */
public class Bach {

  /** Version of Bach, {@link Runtime.Version#parse(String)}-compatible. */
  public static final String VERSION = "2-ea";

  /**
   * Create new Bach instance with default properties.
   *
   * @return new default Bach instance
   */
  public static Bach of() {
    var out = new PrintWriter(System.out, true);
    var err = new PrintWriter(System.err, true);
    return new Bach(out, err, Map.of());
  }

  /**
   * Main entry-point of Bach.
   *
   * @param arguments task name(s) and their argument(s)
   * @throws Error on a non-zero error code
   */
  public static void main(String... arguments) {
    var args = List.of(Util.assigned(arguments, "arguments"));
    var bach = Bach.of();
    var code = bach.main(args);
    if (code != 0) {
      throw new Error("Bach.main(" + Util.join(arguments) + ") failed with error code: " + code);
    }
  }

  final PrintWriter out;
  final PrintWriter err;
  final Map<String, Tool> tools;

  final Runner runner;

  Bach(PrintWriter out, PrintWriter err, Map<String, Tool> tools) {
    this.out = Util.assigned(out, "out");
    this.err = Util.assigned(err, "err");
    this.tools = Util.assigned(tools, "tools");
    this.runner = new Runner();
  }

  /** Main-entry point running tools indicated by the given arguments. */
  public int main(List<String> arguments) {
    out.println("Bach.java " + VERSION);
    out.println("  arguments=" + Util.assigned(arguments, "arguments"));
    out.println("  tools=" + tools);
    for (var name : arguments) {
      var code = runner.run(name);
      if (code != 0) {
        return code;
      }
    }
    return 0;
  }

  /** Run named tool with specified arguments asserting an expected error code. */
  public void run(int expected, String name, Object... arguments) {
    var code = runner.run(name, arguments);
    if (code != expected) {
      var message = "Tool %s(%s) returned %d, but expected %d";
      throw new AssertionError(String.format(message, name, Util.join(arguments), code, expected));
    }
  }

  /** Tool-invoking dispatcher. */
  class Runner {

    /** Run named tool with specified arguments returning an error code. */
    int run(String name, Object... arguments) {
      out.printf(">> %s(%s)%n", name, Util.join(arguments));
      var tool = tools.get(name);
      if (tool != null) {
        return tool.run(arguments);
      }
      var provider = ToolProvider.findFirst(name);
      return provider
          .map(toolProvider -> toolProvider.run(out, err, Util.strings(arguments)))
          .orElse(42);
    }
  }

  /** Custom tool interface. */
  @FunctionalInterface
  public interface Tool {

    default String name() {
      return getClass().getSimpleName();
    }

    int run(Object... arguments);
  }

  /** Static helper. */
  static class Util {

    /** Assigned returns P if P is non-nil and throws an exception if P is nil. */
    static <T> T assigned(T object, String name) {
      return Objects.requireNonNull(object, name + " must not be null");
    }

    /** Join an array of objects into a human-readable string. */
    @SafeVarargs
    static <T> String join(T... objects) {
      var list = new ArrayList<String>();
      for (var object : objects) {
        list.add(Objects.toString(object, "<null>"));
      }
      return list.isEmpty() ? "<empty>" : '"' + String.join("\", \"", list) + '"';
    }

    /** Convert given array of objects to an array of strings. */
    static String[] strings(Object... objects) {
      var list = new ArrayList<String>();
      for (var object : objects) {
        list.add(Objects.toString(object, null));
      }
      return list.toArray(String[]::new);
    }
  }
}
