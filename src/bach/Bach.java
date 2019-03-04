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

import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.spi.ToolProvider;

/** Java Shell Builder. */
class Bach {

  /** Main entry-point throwing runtime exception on error. */
  public static void main(String... args) {
    var actions = new ArrayList<Action>();
    if (args.length == 0) {
      actions.add(Action.Default.BUILD);
    } else {
      var arguments = new ArrayDeque<>(List.of(args));
      while (!arguments.isEmpty()) {
        var argument = arguments.removeFirst();
        var defaultAction = Action.Default.valueOf(argument.toUpperCase());
        actions.add(defaultAction.consume(arguments));
      }
    }
    new Bach().run(actions);
  }

  /** {@code -Debug=true} flag. */
  final boolean debug;

  /** Base path defaults to user's current working directory. */
  final Path base;

  /** Logging helper. */
  final Log log;

  /** Initialize Bach instance using system properties. */
  Bach() {
    this(Boolean.getBoolean("ebug"), Path.of(System.getProperty("bach.base", "")));
  }

  /** Initialize Bach instance in supplied working directory. */
  Bach(boolean debug, Path base) {
    this.debug = debug;
    this.base = base;
    this.log = new Log();
  }

  /** Execute a collection of actions sequentially on this instance. */
  void run(Collection<? extends Action> actions) {
    log.log(Level.DEBUG, String.format("Performing %d action(s)...", actions.size()));
    for (var action : actions) {
      try {
        log.log(Level.TRACE, String.format(">> %s", action));
        action.perform(this);
        log.log(Level.TRACE, String.format("<< %s", action));
      } catch (Throwable throwable) {
        log.log(Level.ERROR, throwable.getMessage());
        throw new Error("Action failed: " + action, throwable);
      }
    }
  }

  /** Execute the named tool. */
  int run(String name, String... args) {
    log.log(Level.DEBUG, String.format("run(%s, %s)", name, List.of(args)));
    var toolProvider = ToolProvider.findFirst(name);
    if (toolProvider.isPresent()) {
      var tool = toolProvider.get();
      log.log(Level.DEBUG, "Running provided tool in-process: " + tool);
      return tool.run(System.out, System.err, args);
    }
    // TODO Find registered tool, like "format", "junit", "maven", "gradle"
    // TODO Find executable via {java.home}/${name}[.exe]
    try {
      var builder = new ProcessBuilder(name).inheritIO();
      builder.command().addAll(List.of(args));
      var process = builder.start();
      log.log(Level.DEBUG, "Running tool in a new process: " + process);
      return process.waitFor();
    } catch (Exception e) {
      throw new Error("Running tool " + name + " failed!", e);
    }
  }

  /** Build all and everything. */
  public void build() throws Exception {
    Thread.sleep(ThreadLocalRandom.current().nextLong(111, 999));
  }

  /** Logging helper. */
  class Log {

    /** Current logging level threshold. */
    Level threshold = debug ? Level.ALL : Level.INFO;

    /** Standard output message consumer. */
    Consumer<String> out = System.out::println;

    /** Error output stream. */
    Consumer<String> err = System.err::println;

    /** Log message unless threshold suppresses it. */
    void log(Level level, String message) {
      if (level.getSeverity() < threshold.getSeverity()) {
        return;
      }
      var consumer = level.getSeverity() < Level.WARNING.getSeverity() ? out : err;
      consumer.accept(message);
    }
  }

  /** Bach consuming action operating via side-effects. */
  @FunctionalInterface
  interface Action {

    /** Performs this action on the given Bach instance. */
    void perform(Bach bach) throws Exception;

    /** Default action delegating to Bach API methods. */
    enum Default implements Action {
      BUILD(Bach::build),

      TOOL(null) {
        @Override
        Action consume(Deque<String> arguments) {
          var name = arguments.removeFirst();
          var args = arguments.toArray(String[]::new);
          arguments.clear();
          return bach -> bach.run(name, args);
        }
      };

      final Action action;

      Default(Action action) {
        this.action = action;
      }

      @Override
      public void perform(Bach bach) throws Exception {
        action.perform(bach);
      }

      Action consume(Deque<String> arguments) {
        return this;
      }
    }
  }
}
