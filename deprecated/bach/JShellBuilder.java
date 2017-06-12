/*
 * Java Shell Builder
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
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.function.*;
import java.util.logging.*;
import java.util.spi.*;
import java.util.stream.*;

/** JShell Builder. */
@SuppressWarnings({
  "WeakerAccess",
  "RedundantIfStatement",
  "UnusedReturnValue",
  "SameParameterValue",
  "SimplifiableIfStatement"
})
public interface JShellBuilder {

  /** Do it all: clean, compile, [...] and test. */
  default void build() {
    clean();
    compile();
    test();
  }

  /** Create and execute command. */
  int call(String executable, Object... arguments);

  /** Delete all generated files. */
  void clean();

  /** Compile all modules. */
  void compile();

  /** Test all modules. */
  void test();

  class Bach implements JShellBuilder {

    final Logger logger;
    final String projectName;
    final PrintStream streamErr = System.err;
    final PrintStream streamOut = System.out;
    final Map<String, ToolProvider> toolProviderMap = new TreeMap<>();

    Bach(Builder builder) {
      this.projectName = builder.name;
      this.logger = Logger.getLogger("Bach");
      if (builder.handler != null) {
        builder.handler.setLevel(builder.level);
        logger.addHandler(builder.handler);
        logger.setUseParentHandlers(false);
      }
      logger.setLevel(builder.level);
      logger.config("projectName = " + projectName);
      logger.info("Bach initialized");
    }

    @Override
    public int call(String executable, Object... arguments) {
      return execute(command(executable, arguments));
    }

    @Override
    public void clean() {
      logger.info("clean");
    }

    /** Create command instance from executable name and optional arguments. */
    Command command(String executable, Object... arguments) {
      return new Command(executable).addAll(arguments);
    }

    @Override
    public void compile() {
      logger.info("compile");
    }

    /** Execute command throwing a runtime exception when the exit value is not zero. */
    int execute(Command command) {
      return execute(command, this::exitValueChecker);
    }

    /** Execute command with supplied exit value checker. */
    int execute(Command command, Consumer<Integer> exitValueChecker) {
      Level level = Level.FINE;
      command.dumpToLog(level);
      String executable = command.executable;
      long start = System.currentTimeMillis();
      Integer exitValue = null;
      ToolProvider providedTool = toolProviderMap.get(executable);
      if (providedTool != null) {
        logger.fine(() -> String.format("executing provided `%s` tool...", executable));
        exitValue = providedTool.run(streamOut, streamErr, command.toArgumentsArray());
      }
      if (exitValue == null) {
        Optional<ToolProvider> tool = ToolProvider.findFirst(executable);
        if (tool.isPresent()) {
          logger.fine(() -> String.format("executing loaded `%s` tool...", executable));
          exitValue = tool.get().run(streamOut, streamErr, command.toArgumentsArray());
        }
      }
      if (exitValue == null) {
        logger.fine(() -> String.format("executing external `%s` tool...", executable));
        ProcessBuilder processBuilder = command.toProcessBuilder().redirectErrorStream(true);
        try {
          Process process = processBuilder.start();
          process.getInputStream().transferTo(streamOut);
          exitValue = process.waitFor();
        } catch (Exception e) {
          if (!logger.isLoggable(level)) {
            command.dumpToLog(Level.SEVERE);
          }
          throw new Error(String.format("executing `%s` failed", executable), e);
        }
      }
      logger.fine(
          () ->
              String.format(
                  "%s finished after %d ms", executable, System.currentTimeMillis() - start));
      exitValueChecker.accept(exitValue);
      return exitValue;
    }

    /** Throw an {@link Error} when exit value is not zero. */
    void exitValueChecker(int value) {
      if (value == 0) {
        return;
      }
      throw new Error(String.format("exit value %d indicates an error", value));
    }

    @Override
    public void test() {
      logger.info("test");
    }
  }

  class Builder {
    Handler handler = buildHandler();
    Level level = Level.FINE;
    String name = Paths.get(".").toAbsolutePath().normalize().getFileName().toString();

    Bach build() {
      return new Bach(this);
    }

    private Handler buildHandler() {
      Handler handler = new ConsoleHandler();
      handler.setFormatter(new SingleLineFormatter());
      return handler;
    }

    Builder handler(Handler handler) {
      this.handler = handler;
      return this;
    }

    Builder level(Level level) {
      this.level = level;
      return this;
    }

    Builder name(String name) {
      this.name = name;
      return this;
    }
  }

  class Command {
    final List<String> arguments = new ArrayList<>();
    int dumpLimit = Integer.MAX_VALUE;
    int dumpOffset = Integer.MAX_VALUE;
    final String executable;
    final Logger logger;

    public Command(String executable) {
      this.executable = executable;
      this.logger = Logger.getLogger("Command");
    }

    /** Conditionally add argument. */
    public Command add(boolean condition, Object argument) {
      if (condition) {
        add(argument);
      }
      return this;
    }

    /** Add single argument with implicit null pointer check. */
    public Command add(Object argument) {
      arguments.add(argument.toString());
      return this;
    }

    /** Add all stream elements joined to a single argument. */
    public Command add(Stream<?> stream, String separator) {
      return add(stream.map(Object::toString).collect(Collectors.joining(separator)));
    }

    //    /** Add all folders joined to a single argument. */
    //    public Command add(Function<Bach.Folder, Path> mapper, Bach.Folder... folders) {
    //      return add(Arrays.stream(folders).map(mapper), File.pathSeparator);
    //    }

    /** Add all arguments from the array. */
    public Command addAll(Object... arguments) {
      for (Object argument : arguments) {
        add(argument);
      }
      return this;
    }

    /** Add all arguments from the stream. */
    public Command addAll(Stream<?> stream) {
      // FIXME "try (stream)" is blocked by https://github.com/google/google-java-format/issues/155
      try {
        stream.forEach(this::add);
      } finally {
        stream.close();
      }
      return this;
    }

    /** Add all files visited by walking specified path recursively. */
    public Command addAll(Path path, Predicate<Path> predicate) {
      try {
        addAll(Files.walk(path).filter(predicate));
      } catch (IOException e) {
        throw new Error(String.format("walking path `%s` failed", path), e);
      }
      return this;
    }

    private void addOption(Object options, java.lang.reflect.Field field) throws Exception {
      // custom generator available?
      try {
        Object result = options.getClass().getDeclaredMethod(field.getName()).invoke(options);
        if (result instanceof List) {
          ((List<?>) result).forEach(this::add);
          return;
        }
      } catch (NoSuchMethodException e) {
        // fall-through
      }
      // additional arguments?
      String name = field.getName();
      Object value = field.get(options);
      if ("additionalArguments".equals(name) && value instanceof List) {
        ((List<?>) value).forEach(this::add);
        return;
      }
      // get (or guess) name and value
      String optionName = "-" + name;
      //      if (field.isAnnotationPresent(Bach.OptionName.class)) {
      //        optionName = field.getAnnotation(Bach.OptionName.class).value();
      //      }
      // just a flag?
      if (field.getType() == boolean.class) {
        if (field.getBoolean(options)) {
          add(optionName);
        }
        return;
      }
      // as-is
      add(optionName);
      add(Objects.toString(value));
    }

    private void addOptionUnchecked(Object options, java.lang.reflect.Field field) {
      try {
        addOption(options, field);
      } catch (Exception e) {
        throw new Error("reflecting options failed for " + options, e);
      }
    }

    /** Reflect and add all options. */
    public Command addOptions(Object options) {
      if (options == null) {
        return this;
      }
      Arrays.stream(options.getClass().getDeclaredFields())
          .sorted(Comparator.comparing(java.lang.reflect.Field::getName))
          .filter(field -> !field.isSynthetic())
          .filter(field -> !java.lang.reflect.Modifier.isStatic(field.getModifiers()))
          .forEach(field -> addOptionUnchecked(options, field));
      return this;
    }

    /** Dump command properties to default logging output. */
    void dumpToLog(Level level) {
      if (logger.isLoggable(level)) {
        return;
      }
      dumpToPrinter((format, args) -> logger.log(level, format, args));
    }

    /** Dump command properties using the provided printer. */
    void dumpToPrinter(BiConsumer<String, Object[]> printer) {
      ListIterator<String> iterator = arguments.listIterator();
      printer.accept("%s", new Object[] {executable});
      while (iterator.hasNext()) {
        String argument = iterator.next();
        int nextIndex = iterator.nextIndex();
        String indent = nextIndex > dumpOffset || argument.startsWith("-") ? "" : "  ";
        printer.accept("%s%s", new Object[] {indent, argument});
        if (nextIndex >= dumpLimit) {
          int last = arguments.size() - 1;
          printer.accept("%s... [omitted %d arguments]", new Object[] {indent, last - nextIndex});
          printer.accept("%s%s", new Object[] {indent, arguments.get(last)});
          break;
        }
      }
    }

    /** Set dump offset and limit. */
    Command mark(int limit) {
      this.dumpOffset = arguments.size();
      this.dumpLimit = arguments.size() + limit;
      return this;
    }

    /** Create new argument array based on this command's arguments. */
    public String[] toArgumentsArray() {
      return arguments.toArray(new String[arguments.size()]);
    }

    /** Create new {@link ProcessBuilder} instance based on this command setup. */
    public ProcessBuilder toProcessBuilder() {
      ArrayList<String> command = new ArrayList<>(1 + arguments.size());
      command.add(executable);
      command.addAll(arguments);
      return new ProcessBuilder(command);
    }
  }

  class SingleLineFormatter extends java.util.logging.Formatter {

    private final DateTimeFormatter instantFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss:SSS").withZone(ZoneId.systemDefault());

    @Override
    public String format(LogRecord record) {
      StringBuilder builder = new StringBuilder();
      builder.append(instantFormatter.format(record.getInstant()));
      builder.append(' ');
      builder.append(record.getSourceMethodName());
      builder.append(' ');
      builder.append(formatMessage(record));
      if (record.getThrown() != null) {
        builder.append(System.lineSeparator());
        builder.append(' ');
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        record.getThrown().printStackTrace(pw);
        pw.close();
        builder.append(sw.getBuffer());
      }
      builder.append(System.lineSeparator());
      return builder.toString();
    }
  }
}
