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
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

/*BODY*/
public class Bach {

  public static String VERSION = "2-ea";

  /**
   * Create new Bach instance with default configuration.
   *
   * @return new default Bach instance
   */
  public static Bach of() {
    var out = new PrintWriter(System.out, true);
    var err = new PrintWriter(System.err, true);
    var verbose = Boolean.getBoolean("ebug") || "".equals(System.getProperty("ebug"));
    return new Bach(out, err, verbose);
  }

  /**
   * Main entry-point.
   *
   * @param args List of API method or tool names.
   */
  public static void main(String... args) {
    var bach = Bach.of();
    try {
      bach.main(args.length == 0 ? List.of("build") : List.of(args));
    } catch (Throwable throwable) {
      bach.err.printf("Bach.java failed: %s%n", throwable.getMessage());
      if (Boolean.getBoolean("ebug")) {
        throwable.printStackTrace(bach.err);
      }
    }
  }

  /** Text-output writer. */
  private final PrintWriter out, err;
  /** Be verbose. */
  private final boolean verbose;

  public Bach(PrintWriter out, PrintWriter err, boolean verbose) {
    this.out = Util.requireNonNull(out, "out");
    this.err = Util.requireNonNull(err, "err");
    this.verbose = verbose;
    log("New instance initialized: %s", this);
  }

  private void log(String format, Object... args) {
    if (verbose) out.println(String.format(format, args));
  }

  void main(List<String> args) {
    log("Parsing argument(s): %s", args);

    abstract class Action implements Runnable {
      private final String description;

      private Action(String description) {
        this.description = description;
      }

      @Override
      public String toString() {
        return description;
      }
    }

    var arguments = new ArrayDeque<>(args);
    var actions = new ArrayList<Action>();
    var lookup = MethodHandles.publicLookup();
    var type = MethodType.methodType(void.class);
    while (!arguments.isEmpty()) {
      var name = arguments.pop();
      // Try Bach API method w/o parameter -- single argument is consumed
      try {
        try {
          lookup.findVirtual(Object.class, name, type);
        } catch (NoSuchMethodException e) {
          var handle = lookup.findVirtual(getClass(), name, type);
          actions.add(
              new Action(name + "()") {
                @Override
                public void run() {
                  try {
                    handle.invokeExact(Bach.this);
                  } catch (Throwable t) {
                    throw new AssertionError("Running method failed: " + handle, t);
                  }
                }
              });
          continue;
        }
      } catch (ReflectiveOperationException e) {
        // fall through
      }
      // Try provided tool -- all remaining arguments are consumed
      var tool = ToolProvider.findFirst(name);
      if (tool.isPresent()) {
        var options = List.copyOf(arguments);
        actions.add(
            new Action(name + " " + options) {
              @Override
              public void run() {
                var strings = arguments.stream().map(Object::toString).toArray(String[]::new);
                log("%s %s", name, String.join(" ", strings));
                var code = tool.get().run(out, err, strings);
                if (code != 0) {
                  throw new AssertionError(name + " returned non-zero exit code: " + code);
                }
              }
            });
        break;
      }
      throw new IllegalArgumentException("Unsupported argument: " + name);
    }
    log("Running %d action(s): %s", actions.size(), actions);
    actions.forEach(Runnable::run);
  }

  String getBanner() {
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

  public void help() {
    out.println(getBanner());
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

  public void build() {
    info();
  }

  public void info() {
    out.printf("Bach (%s)%n", VERSION);
  }

  public void version() {
    out.println(VERSION);
  }
}
