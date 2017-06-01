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
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import java.util.logging.*;
import java.util.spi.*;
import java.util.stream.*;

/**
 * Java Shell Builder.
 *
 * @noinspection WeakerAccess, RedundantIfStatement, UnusedReturnValue, unused, SameParameterValue
 */
class JavaShellBuilder {

  Charset charset = StandardCharsets.UTF_8;
  Map<Folder, Path> folders = new EnumMap<>(Folder.class);
  final Log log = new Log();
  PrintStream streamErr = System.err;
  PrintStream streamOut = System.out;
  final Tool tool = new Tool();

  /** Create command instance from executable name and optional arguments. */
  Command command(String executable, Object... arguments) {
    return new Command(executable).addAll(arguments);
  }

  /** Compile all java modules (main and test). */
  void compile() {
    log.tag("compile");
    log.println(Level.CONFIG, "folder %s", folders.keySet());
    // TODO javac(Paths.get("src"), path(Folder.TARGET_COMPILE_MAIN),
    // tool.defaultJavacOptions.get());
  }

  /** Create and execute command. */
  void execute(String executable, Object... arguments) {
    log.tag("execute");
    command(executable, arguments).execute();
  }

  /** Read Java class and interface definitions and compile them into bytecode and class files. */
  void javac(Path moduleSourcePath, Path destinationPath, Tool.JavacOptions options) {
    log.tag("javac");
    log.check(Files.exists(moduleSourcePath), "path `%s` does not exist", moduleSourcePath);
    Command command = command("javac");
    command.addOptions(options);
    // sets the destination directory for class files
    command.add("-d");
    command.add(destinationPath);
    // specify where to find input source files for multiple modules
    command.add("--module-source-path");
    command.add(moduleSourcePath);
    command.markDumpLimit(10);
    try {
      command.addAll(Files.walk(moduleSourcePath, 1).filter(Util::isJavaSourceFile));
    } catch (IOException e) {
      log.error(e, "gathering java source files in %s", moduleSourcePath);
    }
    command.execute();
  }

  /** Resolve path for given folder. */
  Path path(Folder folder) {
    Path path = folders.getOrDefault(folder, folder.path);
    return folder.parent == null ? path : path(folder.parent).resolve(path);
  }

  /** Override default folder path with a custom path. */
  void set(Folder folder, Path path) {
    folders.put(folder, path);
  }

  /** Set log level threshold. */
  void set(Level level) {
    log.level(level);
  }

  class Command {
    final List<String> arguments = new ArrayList<>();
    int dumpLimit = Integer.MAX_VALUE;
    int dumpOffset = Integer.MAX_VALUE;
    final String executable;

    Command(String executable) {
      this.executable = executable;
    }

    /** Conditionally add argument. */
    Command add(boolean condition, Object argument) {
      if (condition) {
        add(argument);
      }
      return this;
    }

    /** Add single argument with implicit null pointer check. */
    Command add(Object argument) {
      arguments.add(argument.toString());
      return this;
    }

    /** Add all arguments from the array. */
    Command addAll(Object... arguments) {
      for (Object argument : arguments) {
        add(argument);
      }
      return this;
    }

    /** Add all arguments from the stream. */
    Command addAll(Stream<?> stream) {
      // FIXME "try (stream)" is blocked by https://github.com/google/google-java-format/issues/155
      try {
        stream.forEach(this::add);
      } finally {
        stream.close();
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
      // guess key and value
      String optionKey = "-" + name;
      // just a flag?
      if (field.getType() == boolean.class) {
        if (field.getBoolean(options)) {
          add(optionKey);
        }
        return;
      }
      // as-is
      add(optionKey);
      add(Objects.toString(value));
    }

    /** Reflect and add all options. */
    Command addOptions(Object options) {
      if (options == null) {
        return this;
      }
      try {
        for (java.lang.reflect.Field field : options.getClass().getDeclaredFields()) {
          // skip static and synthetic fields (like pointer to "this", "super", etc)
          if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
            continue;
          }
          addOption(options, field);
        }
      } catch (Exception e) {
        log.error(e, "reflecting options failed for %s", options);
      }
      return this;
    }

    /** Dump command properties to default logging output. */
    void dumpToLog(Level level) {
      if (log.isLevelSuppressed(level)) {
        return;
      }
      dumpToPrinter((format, args) -> log.println(level, format, args));
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

    /** Execute command throwing a runtime exception when the exit value is not zero. */
    void execute() {
      execute(this::exitValueChecker);
    }

    /** Execute command with supplied exit value checker. */
    void execute(Consumer<Integer> exitValueChecker) {
      dumpToLog(Level.FINE);
      try {
        long start = System.currentTimeMillis();
        int exitValue;
        Optional<ToolProvider> tool = ToolProvider.findFirst(executable);
        if (tool.isPresent()) {
          log.fine("executing provided `%s` tool in-process...", executable);
          exitValue = tool.get().run(streamOut, streamErr, toArgumentsArray());
        } else {
          log.fine("executing external `%s` tool in new process...", executable);
          Process process = toProcessBuilder().redirectErrorStream(true).start();
          process.getInputStream().transferTo(streamOut);
          exitValue = process.waitFor();
        }
        log.fine("%s finished after %d ms", executable, System.currentTimeMillis() - start);
        exitValueChecker.accept(exitValue);
      } catch (Exception e) {
        if (log.isLevelSuppressed(Level.FINE)) {
          dumpToLog(Level.SEVERE);
        }
        throw new Error(String.format("execution of %s failed", executable), e);
      }
    }

    /** Throw an {@link AssertionError} when exit value is not zero. */
    void exitValueChecker(int value) {
      if (value == 0) {
        return;
      }
      throw new AssertionError(String.format("exit value %d indicates an error", value));
    }

    /** Set dump offset and limit. */
    Command markDumpLimit(int limit) {
      this.dumpOffset = arguments.size();
      this.dumpLimit = arguments.size() + limit;
      return this;
    }

    /** Create new argument array based on this command's arguments. */
    String[] toArgumentsArray() {
      return arguments.toArray(new String[arguments.size()]);
    }

    /** Create new {@link ProcessBuilder} instance based on this command setup. */
    ProcessBuilder toProcessBuilder() {
      ArrayList<String> command = new ArrayList<>(1 + arguments.size());
      command.add(executable);
      command.addAll(arguments);
      return new ProcessBuilder(command);
    }
  }

  enum Folder {
    JDK_HOME(Util.buildJdkHome()),
    JDK_HOME_BIN(JDK_HOME, Paths.get("bin")),
    JDK_HOME_MODS(JDK_HOME, Paths.get("jmods")),
    //
    AUXILIARY(Paths.get(".bach")),
    DEPENDENCIES(AUXILIARY, Paths.get("dependencies")),
    TOOLS(AUXILIARY, Paths.get("tools")),
    //
    TARGET(Paths.get("target", "bach")),
    TARGET_COMPILE_MAIN(TARGET, Paths.get("main", "java"));

    final Folder parent;
    final Path path;

    Folder(Folder parent, Path path) {
      this.parent = parent;
      this.path = path;
    }

    Folder(Path path) {
      this(null, path);
    }
  }

  class Log {

    String tag = "init";
    int threshold = Level.FINE.intValue();

    /** Check condition and on false throw an {@link AssertionError}. */
    void check(boolean condition, String format, Object... args) {
      if (condition) {
        return;
      }
      throw new AssertionError(String.format(format, args));
    }

    <T> T error(Throwable cause, String format, Object... args) {
      String message = String.format(format, args);
      printTagAndMessage(Level.SEVERE, message);
      throw new Error(message, cause);
    }

    /** Print message at level: {@link Level#FINE} */
    void fine(String format, Object... args) {
      println(Level.FINE, format, args);
    }

    /** Print message at level: {@link Level#INFO} */
    void info(String format, Object... args) {
      println(Level.INFO, format, args);
    }

    /** Return {@code true} if the level is not suppressed. */
    boolean isLevelActive(Level level) {
      return !isLevelSuppressed(level);
    }

    /** Return {@code true} if the level value is below the current threshold. */
    boolean isLevelSuppressed(Level level) {
      return level.intValue() < threshold;
    }

    /** Set current threshold to the value reported by the passed level instance. */
    void level(Level level) {
      this.threshold = level.intValue();
    }

    /** Print formatted message line if the level is active. */
    void println(Level level, String format, Object... args) {
      if (isLevelSuppressed(level)) {
        return;
      }
      if (args.length == 1 && args[0] instanceof Iterable) {
        for (Object arg : (Iterable<?>) args[0]) {
          if (arg instanceof Folder) {
            arg = arg + " -> " + JavaShellBuilder.this.path((Folder) arg);
          }
          printTagAndMessage(level, format, arg);
        }
        return;
      }
      printTagAndMessage(level, format, args);
    }

    /** Print tag and formatted message line. */
    private void printTagAndMessage(Level level, String format, Object... args) {
      if (threshold < Level.INFO.intValue()) {
        streamOut.printf("%6s|", level.getName().toLowerCase());
      }
      streamOut.printf("%7s| ", tag);
      streamOut.println(String.format(format, args));
    }

    /** Set current log tag if it differs from the old one. */
    void tag(String tag) {
      if (!Objects.equals(this.tag, tag)) {
        this.tag = tag;
        println(Level.CONFIG, "");
      }
    }
  }

  class Tool {

    class JavacOptions {
      /** User-defined arguments. */
      List<String> additionalArguments = Collections.emptyList();

      /** Output source locations where deprecated APIs are used. */
      boolean deprecation = true;

      /** Specify character encoding used by source files. */
      Charset encoding = JavaShellBuilder.this.charset;

      /** Terminate compilation if warnings occur. */
      boolean failOnWarnings = true;

      /** Specify where to find application modules. */
      List<Path> modulePaths = List.of(JavaShellBuilder.this.path(Folder.DEPENDENCIES));

      /** Generate metadata for reflection on method parameters. */
      boolean parameters = true;

      /** Output messages about what the compiler is doing. */
      boolean verbose = JavaShellBuilder.this.log.isLevelActive(Level.FINEST);

      List<String> encoding() {
        if (Charset.defaultCharset().equals(encoding)) {
          return Collections.emptyList();
        }
        return List.of("-encoding", encoding.name());
      }

      List<String> failOnWarnings() {
        return failOnWarnings ? List.of("-Werror") : Collections.emptyList();
      }

      List<String> modulePaths() {
        return List.of("--module-path", Util.join(modulePaths));
      }
    }

    Supplier<Tool.JavacOptions> defaultJavacOptions = JavacOptions::new;
  }

  interface Util {

    /** Return path to JDK installation directory. */
    static Path buildJdkHome() {
      // try current process information: <JDK_HOME>/bin/java[.exe]
      Path executable = ProcessHandle.current().info().command().map(Paths::get).orElse(null);
      if (executable != null) {
        Path path = executable.getParent(); // <JDK_HOME>/bin
        if (path != null) {
          return path.getParent(); // <JDK_HOME>
        }
      }
      // next, examine system environment...
      String jdkHome = System.getenv("JDK_HOME");
      if (jdkHome != null) {
        return Paths.get(jdkHome);
      }
      String javaHome = System.getenv("JAVA_HOME");
      if (javaHome != null) {
        return Paths.get(javaHome);
      }
      // still here? not so good... try with default (not-existent) path
      return Paths.get("jdk-" + Runtime.version().major());
    }

    /** Return {@code true} if the path points to a Java compilation unit. */
    static boolean isJavaSourceFile(Path path) {
      if (!Files.isRegularFile(path)) {
        return false;
      }
      if (!path.getFileName().toString().endsWith(".java")) {
        return false;
      }
      return true;
    }

    /** Join paths to a single representation using system-dependent path-separator character. */
    static String join(List<Path> paths) {
      List<String> locations = paths.stream().map(Object::toString).collect(Collectors.toList());
      return String.join(File.pathSeparator, locations);
    }
  }
}
