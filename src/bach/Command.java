/*
 * Bach - Java Shell Builder
 * Copyright (C) 2017 Christian Stein
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

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.function.*;
import java.util.spi.*;
import java.util.stream.*;

class Command {

  interface Visitor extends Consumer<Command> {}

  static Visitor visit(Consumer<Command> consumer) {
    return consumer::accept;
  }

  /** Command option annotation. */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  @interface Option {
    String value();
  }

  /** Type-safe helper for adding common options. */
  class Helper {

    @SuppressWarnings("unused")
    void patchModule(Map<String, List<Path>> patchModule) {
      patchModule.forEach(this::addPatchModule);
    }

    private void addPatchModule(String module, List<Path> paths) {
      if (paths.isEmpty()) {
        throw new AssertionError("expected at least one patch path entry for " + module);
      }
      var patches =
          paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
      add("--patch-module");
      add(module + "=" + patches);
    }
  }

  final String executable;
  final List<String> arguments = new ArrayList<>();
  private final Helper helper = new Helper();
  private int dumpLimit = Integer.MAX_VALUE;
  private int dumpOffset = Integer.MAX_VALUE;
  private PrintStream out = System.out;
  private PrintStream err = System.err;
  private Map<String, ToolProvider> tools = Collections.emptyMap();
  private boolean executableSupportsArgumentFile = false;

  /** Initialize this command instance. */
  Command(String executable) {
    this.executable = executable;
  }

  /** Add single argument composed of joined path names using {@link File#pathSeparator}. */
  Command add(Collection<Path> paths) {
    return add(paths.stream(), File.pathSeparator);
  }

  /** Add single non-null argument. */
  Command add(Object argument) {
    if (argument instanceof Visitor) {
      ((Visitor) argument).accept(this);
      return this;
    }
    arguments.add(argument.toString());
    return this;
  }

  /** Add single argument composed of all stream elements joined by specified separator. */
  Command add(Stream<?> stream, String separator) {
    return add(stream.map(Object::toString).collect(Collectors.joining(separator)));
  }

  /** Add all arguments by invoking {@link #add(Object)} for each element. */
  Command addAll(Iterable<?> arguments) {
    arguments.forEach(this::add);
    return this;
  }

  /** Add all files visited by walking specified root path recursively. */
  Command addAll(Path root, Predicate<Path> predicate) {
    try (var stream = Files.walk(root).filter(predicate)) {
      stream.forEach(this::add);
    } catch (IOException e) {
      throw new UncheckedIOException("walking path `" + root + "` failed", e);
    }
    return this;
  }

  /** Add all files visited by walking specified root paths recursively. */
  Command addAll(Collection<Path> roots, Predicate<Path> predicate) {
    roots.forEach(root -> addAll(root, predicate));
    return this;
  }

  /** Add all .java source files by walking specified root paths recursively. */
  Command addAllJavaFiles(List<Path> roots) {
    Predicate<Path> java = path -> path.getFileName().toString().endsWith(".java");
    return addAll(roots, java);
  }

  /** Add all reflected options. */
  Command addAllOptions(Object options) {
    return addAllOptions(options, UnaryOperator.identity());
  }

  /** Add all reflected options after a custom stream operator did its work. */
  Command addAllOptions(Object options, UnaryOperator<Stream<Field>> operator) {
    var stream =
        Arrays.stream(options.getClass().getDeclaredFields())
            .filter(field -> !field.isSynthetic())
            .filter(field -> !java.lang.reflect.Modifier.isStatic(field.getModifiers()))
            .filter(field -> !java.lang.reflect.Modifier.isPrivate(field.getModifiers()))
            .filter(field -> !java.lang.reflect.Modifier.isTransient(field.getModifiers()));
    stream = operator.apply(stream);
    stream.forEach(field -> addOptionUnchecked(options, field));
    return this;
  }

  private void addOption(Object options, Field field) throws ReflectiveOperationException {
    // custom option visitor method declared?
    try {
      options.getClass().getDeclaredMethod(field.getName(), Command.class).invoke(options, this);
      return;
    } catch (NoSuchMethodException e) {
      // fall-through
    }
    // get the field's value
    var value = field.get(options);
    // skip null field value
    if (value == null) {
      return;
    }
    // skip empty collections
    if (value instanceof Collection && ((Collection) value).isEmpty()) {
      return;
    }
    // common add helper available?
    try {
      Helper.class.getDeclaredMethod(field.getName(), field.getType()).invoke(helper, value);
      return;
    } catch (NoSuchMethodException e) {
      // fall-through
    }
    // get or generate option name
    var optional = Optional.ofNullable(field.getAnnotation(Option.class));
    var optionName = optional.map(Option::value).orElse(getOptionName(field.getName()));
    // is it an omissible boolean flag?
    if (field.getType() == boolean.class) {
      if (field.getBoolean(options)) {
        add(optionName);
      }
      return;
    }
    // add option name only if it is not empty
    if (!optionName.isEmpty()) {
      add(optionName);
    }
    // is value a collection of paths?
    if (value instanceof Collection) {
      if (((Collection) value).iterator().next() instanceof Path) {
        @SuppressWarnings("unchecked")
        var path = (Collection<Path>) value;
        add(path);
        return;
      }
    }
    // is value a charset?
    if (value instanceof Charset) {
      add(((Charset) value).name());
      return;
    }
    // finally, add string representation of the value
    add(value.toString());
  }

  private void addOptionUnchecked(Object options, Field field) {
    try {
      addOption(options, field);
    } catch (ReflectiveOperationException e) {
      throw new Error("reflecting option from field '" + field + "' failed for " + options, e);
    }
  }

  private String getOptionName(String fieldName) {
    var hasUppercase = !fieldName.equals(fieldName.toLowerCase());
    var defaultName = new StringBuilder();
    if (hasUppercase) {
      defaultName.append("--");
      fieldName
          .chars()
          .forEach(
              i -> {
                if (Character.isUpperCase(i)) {
                  defaultName.append('-');
                  defaultName.append((char) Character.toLowerCase(i));
                } else {
                  defaultName.append((char) i);
                }
              });
    } else {
      defaultName.append('-');
      defaultName.append(fieldName.replace('_', '-'));
    }
    return defaultName.toString();
  }

  /** Dump command executables and arguments using the provided string consumer. */
  Command dump(Consumer<String> consumer) {
    var iterator = arguments.listIterator();
    consumer.accept(executable);
    while (iterator.hasNext()) {
      var argument = iterator.next();
      var nextIndex = iterator.nextIndex();
      var indent = nextIndex > dumpOffset || argument.startsWith("-") ? "" : "  ";
      consumer.accept(indent + argument);
      if (nextIndex > dumpLimit) {
        var last = arguments.size() - 1;
        var diff = last - nextIndex;
        if (diff > 1) {
          consumer.accept(indent + "... [omitted " + diff + " arguments]");
        }
        consumer.accept(indent + arguments.get(last));
        break;
      }
    }
    return this;
  }

  /** Set dump offset and limit. */
  Command mark(int limit) {
    if (limit < 0) {
      throw new IllegalArgumentException("limit must be greater then zero: " + limit);
    }
    this.dumpOffset = arguments.size();
    this.dumpLimit = arguments.size() + limit;
    return this;
  }

  /** Set argument file support. */
  Command setExecutableSupportsArgumentFile(boolean executableSupportsArgumentFile) {
    this.executableSupportsArgumentFile = executableSupportsArgumentFile;
    return this;
  }

  /** Set standard output and error streams. */
  Command setStandardStreams(PrintStream out, PrintStream err) {
    this.out = out;
    this.err = err;
    return this;
  }

  /** Put the tool into the internal map of tools. */
  Command setToolProvider(ToolProvider tool) {
    if (tools == Collections.EMPTY_MAP) {
      tools = new TreeMap<>();
    }
    tools.put(tool.name(), tool);
    return this;
  }

  /** Create new argument array based on this command's arguments. */
  String[] toArgumentsArray() {
    return arguments.toArray(new String[0]);
  }

  /** Create new {@link ProcessBuilder} instance based on this command setup. */
  ProcessBuilder toProcessBuilder() {
    List<String> strings = new ArrayList<>(1 + arguments.size());
    // TODO strings.add(Basics.resolveJdkTool(executable).map(Path::toString).orElse(executable));
    strings.add(executable);
    strings.addAll(arguments);
    var commandLineLength = String.join(" ", strings).length();
    if (commandLineLength > 32000) {
      if (executableSupportsArgumentFile) {
        var timestamp = Instant.now().toString().replace("-", "").replace(":", "");
        var prefix = executable + "-arguments-" + timestamp + "-";
        try {
          var tempFile = Files.createTempFile(prefix, ".txt");
          strings = List.of(executable, "@" + Files.write(tempFile, arguments));
        } catch (IOException e) {
          throw new UncheckedIOException("creating temporary arguments file failed", e);
        }
      } else {
        err.println(
            String.format(
                "large command line (%s) detected, but %s does not support @argument file",
                commandLineLength, executable));
      }
    }
    var processBuilder = new ProcessBuilder(strings);
    processBuilder.redirectErrorStream(true);
    return processBuilder;
  }

  /**
   * Run this command.
   *
   * @throws AssertionError if the execution result is not zero
   */
  void run() {
    var result = run(UnaryOperator.identity(), this::toProcessBuilder);
    var successful = result == 0;
    if (successful) {
      return;
    }
    throw new AssertionError("expected an exit code of zero, but got: " + result);
  }

  /**
   * Runs an instance of the tool, returning zero for a successful run.
   *
   * @return the result of executing the tool. A return value of 0 means the tool did not encounter
   *     any errors; any other value indicates that at least one error occurred during execution.
   */
  int run(UnaryOperator<ToolProvider> operator, Supplier<ProcessBuilder> supplier) {
    //        if (log.isEnabled()) {
    //            List<String> lines = new ArrayList<>();
    //            dump(lines::add);
    //            log.info("running %s with %d argument(s)", executable, arguments.size());
    //            log.verbose("%s", String.join("\n", lines));
    //        }
    var systemTool = ToolProvider.findFirst(executable).orElse(null);
    var tool = tools.getOrDefault(executable, systemTool);
    if (tool != null) {
      return operator.apply(tool).run(out, err, toArgumentsArray());
    }
    var processBuilder = supplier.get();
    //        if (log.isEnabled()) {
    //            String actual = processBuilder.command().get(0);
    //            if (!executable.equals(actual)) {
    //                log.verbose("replaced %s with %s", executable, actual);
    //            }
    //        }
    try {
      var process = processBuilder.start();
      process.getInputStream().transferTo(out);
      return process.waitFor();
    } catch (IOException | InterruptedException e) {
      throw new Error("executing `" + executable + "` failed", e);
    }
  }
}
