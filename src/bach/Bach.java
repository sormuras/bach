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

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

/** Java Shell Builder. */
@SuppressWarnings("WeakerAccess")
public class Bach {

  /** Version of Bach, {@link Runtime.Version#parse(String)}-compatible. */
  public static final String VERSION = "2-ea";

  /**
   * Main entry-point of Bach.
   *
   * @param arguments task name(s) and their argument(s)
   * @throws Error on a non-zero error code
   */
  public static void main(String... arguments) {
    var bach = new Bach();
    var args = List.of(arguments);
    var code = bach.main(args);
    if (code != 0) {
      throw new Error("Bach (" + args + ") failed with error code: " + code);
    }
  }

  private final PrintWriter out;
  private final PrintWriter err;
  private final boolean verbose;
  private final Path home;
  private final Path work;
  private final Runner runner;

  Bach() {
    this(new PrintWriter(System.out, true), new PrintWriter(System.err, true));
  }

  Bach(PrintWriter out, PrintWriter err) {
    this(
        out,
        err,
        Path.of(System.getProperty("bach.home", "")),
        Path.of(System.getProperty("bach.work", "")),
        Boolean.getBoolean("bach.verbose"));
  }

  Bach(PrintWriter out, PrintWriter err, Path home, Path work, boolean verbose) {
    this.out = out;
    this.err = err;
    this.home = home;
    this.work = work;
    this.verbose = verbose;
    this.runner = new Runner();
    log("Bach %s initialized.", VERSION);
  }

  void log(String format, Object... args) {
    if (verbose) {
      out.println(String.format(format, args));
    }
  }

  /** Print runtime and project-related information. */
  public void info() {
    log("Bach::info()");
    out.println("Bach information");
    out.println("  out = " + out);
    out.println("  err = " + err);
    out.println("  home = '" + home + "'");
    out.println("  work = '" + work + "'");
    out.println("  verbose = " + verbose);
  }

  /** Build the project.  */
  public void build() {
    log("Bach::build()");
    if (verbose) {
      info();
    }
    // compile, jar, package, document, ...
  }

  /** Main-entry point converting strings to commands and executing each. */
  int main(List<String> arguments) {
    log("Bach::main(%s)", arguments);
    if (arguments.isEmpty()) {
      build();
      return 0;
    }
    var commands = runner.commands(arguments);
    return runner.run(commands);
  }

  /** Runtime context. */
  class Runner {

    List<Command> commands(List<String> strings) {
      var commands = new ArrayList<Command>();
      var deque = new ArrayDeque<>(strings);
      while (!deque.isEmpty()) {
        var string = deque.removeFirst();
        if ("tool".equals(string)) {
          var tool = deque.removeFirst();
          commands.add(new Command(tool).addEach(deque));
          break;
        }
        commands.add(new Command(string));
      }
      return commands;
    }

    /** Run given list of of commands sequentially and fail-fast on non-zero result. */
    int run(List<Command> commands) {
      log("Running %s command(s): %s", commands.size(), commands);
      for (var command : commands) {
        var code = runner.run(command);
        if (code != 0) {
          return code;
        }
      }
      return 0;
    }

    /** Run given command. */
    int run(Command command) {
      return run(command.name, command.toStringArray());
    }

    /**
     * Run named tool, as loaded by {@link java.util.ServiceLoader} using the system class loader.
     */
    int run(String name, String... args) {
      var providedTool = ToolProvider.findFirst(name);
      if (providedTool.isPresent()) {
        var tool = providedTool.get();
        log("Running provided tool in-process: " + tool);
        return run(tool, args);
      }

      try {
        var method = Bach.class.getMethod(name); // no parameters
        log("Invoking instance method: " + method);
        var result = method.invoke(Bach.this); // no arguments
        return result instanceof Number ? ((Number) result).intValue() : 0;
      } catch (NoSuchMethodException e) {
        // fall-through
      } catch (ReflectiveOperationException e) {
        e.printStackTrace(err);
        return 1;
      }

      log("Starting new process for '%s'", name);
      var processBuilder = newProcessBuilder(name);
      processBuilder.command().addAll(List.of(args));
      return run(processBuilder);
    }

    /** Run provided tool. */
    int run(ToolProvider tool, String... args) {
      log("Bach::run(%s, %s)", tool.name(), String.join(", ", args));
      var code = tool.run(out, err, args);
      if (code == 0) {
        log("Tool '%s' successfully run.", tool.name());
      }
      return code;
    }

    /** Create new process builder for the given command and inherit IO from current process. */
    ProcessBuilder newProcessBuilder(String command) {
      var builder = new ProcessBuilder(command).inheritIO();
      builder.environment().put("BACH_VERSION", Bach.VERSION);
      builder.environment().put("BACH_HOME", home.toString());
      builder.environment().put("BACH_WORK", work.toString());
      return builder;
    }

    int run(ProcessBuilder builder) {
      log("Bach::run(%s)", builder);
      try {
        var process = builder.start();
        var code = process.waitFor();
        if (code == 0) {
          log("Process '%s' successfully terminated.", process);
        }
        return code;
      } catch (Exception e) {
        throw new Error("Starting process failed: " + e);
      }
    }
  }

  /** Command-line program argument list builder. */
  static class Command {

    final String name;
    final List<String> list = new ArrayList<>();

    /** Initialize Command instance with zero or more arguments. */
    Command(String name, Object... args) {
      this.name = name;
      addEach(args);
    }

    /** Add single argument by invoking {@link Object#toString()} on the given argument. */
    Command add(Object argument) {
      list.add(argument.toString());
      return this;
    }

    /** Add two arguments by invoking {@link #add(Object)} for the key and value elements. */
    Command add(Object key, Object value) {
      return add(key).add(value);
    }

    /** Add two arguments, i.e. the key and the paths joined by system's path separator. */
    Command add(Object key, Collection<Path> paths) {
      return add(key, paths, UnaryOperator.identity());
    }

    /** Add two arguments, i.e. the key and the paths joined by system's path separator. */
    Command add(Object key, Collection<Path> paths, UnaryOperator<String> operator) {
      var stream = paths.stream() /*.filter(Files::isDirectory)*/.map(Object::toString);
      return add(key, operator.apply(stream.collect(Collectors.joining(File.pathSeparator))));
    }

    /** Add all arguments by invoking {@link #add(Object)} for each element. */
    Command addEach(Object... arguments) {
      return addEach(List.of(arguments));
    }

    /** Add all arguments by invoking {@link #add(Object)} for each element. */
    Command addEach(Iterable<?> arguments) {
      arguments.forEach(this::add);
      return this;
    }

    /** Add a single argument iff the conditions is {@code true}. */
    Command addIff(boolean condition, Object argument) {
      return condition ? add(argument) : this;
    }

    /** Add two arguments iff the conditions is {@code true}. */
    Command addIff(boolean condition, Object key, Object value) {
      return condition ? add(key, value) : this;
    }

    /** Let the consumer visit, usually modify, this instance iff the conditions is {@code true}. */
    Command addIff(boolean condition, Consumer<Command> visitor) {
      if (condition) {
        visitor.accept(this);
      }
      return this;
    }

    @Override
    public String toString() {
      var args = list.isEmpty() ? "<empty>" : "'" + String.join("', '", list) + "'";
      return "Command{name='" + name + "', list=[" + args + "]}";
    }

    /** Returns an array of {@link String} containing all of the collected arguments. */
    String[] toStringArray() {
      return list.toArray(String[]::new);
    }
  }
}
