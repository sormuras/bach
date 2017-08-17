/* THIS FILE IS GENERATED -- 2017-08-17T19:50:27.887892900Z */
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
import java.net.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.List;
import java.util.function.*;
import java.util.spi.*;
import java.util.stream.*;

/**
 * You can use the foundation JDK tools and commands to create and build applications.
 *
 * @see <a
 *     href="https://docs.oracle.com/javase/9/tools/main-tools-create-and-build-applications.htm">Main
 *     Tools to Create and Build Applications</a>
 */
interface JdkTool {

  /** Command option annotation. */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  @interface Option {
    String value();
  }

  /** Command builder and tool executor support. */
  class Command {

    final List<String> arguments = new ArrayList<>();
    private int dumpLimit = Integer.MAX_VALUE;
    private int dumpOffset = Integer.MAX_VALUE;
    final String executable;

    Command(String executable) {
      this.executable = executable;
    }

    /** Add single argument composed of joined path names using {@link File#pathSeparator}. */
    Command add(Collection<Path> paths) {
      return add(paths.stream(), File.pathSeparator);
    }

    /** Add single non-null argument. */
    Command add(Object argument) {
      arguments.add(argument.toString());
      return this;
    }

    /** Add single argument composed of all stream elements joined by specified separator. */
    Command add(Stream<?> stream, String separator) {
      return add(stream.map(Object::toString).collect(Collectors.joining(separator)));
    }

    /** Add all files visited by walking specified root paths recursively. */
    Command addAll(Collection<Path> roots, Predicate<Path> predicate) {
      roots.forEach(root -> addAll(root, predicate));
      return this;
    }

    /** Add all arguments by invoking {@link #add(Object)} for each element. */
    Command addAll(Iterable<?> arguments) {
      arguments.forEach(this::add);
      return this;
    }

    /** Add all files visited by walking specified root path recursively. */
    Command addAll(Path root, Predicate<Path> predicate) {
      try (Stream<Path> stream = Files.walk(root).filter(predicate)) {
        stream.forEach(this::add);
      } catch (IOException e) {
        throw new Error("walking path `" + root + "` failed", e);
      }
      return this;
    }

    /** Add all reflected options. */
    Command addAllOptions(Object options) {
      return addAllOptions(options, UnaryOperator.identity());
    }

    /** Add all reflected options after a custom stream operator did its work. */
    Command addAllOptions(Object options, UnaryOperator<Stream<java.lang.reflect.Field>> operator) {
      Stream<java.lang.reflect.Field> stream =
          Arrays.stream(options.getClass().getDeclaredFields())
              .filter(field -> !field.isSynthetic())
              .filter(field -> !java.lang.reflect.Modifier.isStatic(field.getModifiers()))
              .filter(field -> !java.lang.reflect.Modifier.isPrivate(field.getModifiers()))
              .filter(field -> !java.lang.reflect.Modifier.isTransient(field.getModifiers()));
      stream = operator.apply(stream);
      stream.forEach(field -> addOptionUnchecked(options, field));
      return this;
    }

    private void addOption(Object options, Field field) throws IllegalAccessException {
      // custom option visitor method declared?
      try {
        options.getClass().getDeclaredMethod(field.getName(), Command.class).invoke(options, this);
        return;
      } catch (NoSuchMethodException e) {
        // fall-through
      } catch (InvocationTargetException e) {
        throw new Error(e);
      }
      // get or generate option name
      Optional<Option> optional = Optional.ofNullable(field.getAnnotation(Option.class));
      String optionName = optional.map(Option::value).orElse(generateName(field.getName()));
      // is it an omissible boolean flag?
      if (field.getType() == boolean.class) {
        if (field.getBoolean(options)) {
          add(optionName);
        }
        return;
      }
      Object value = field.get(options);
      // skip null field value
      if (value == null) {
        return;
      }
      // skip empty collections
      if (value instanceof Collection && ((Collection) value).isEmpty()) {
        return;
      }
      // add option name only if it is not empty
      if (!optionName.isEmpty()) {
        add(optionName);
      }
      // is value a collection of paths?
      if (value instanceof Collection) {
        try {
          @SuppressWarnings("unchecked")
          Collection<Path> path = (Collection<Path>) value;
          add(path);
          return;
        } catch (ClassCastException e) {
          // fall-through
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

    private void addOptionUnchecked(Object options, java.lang.reflect.Field field) {
      try {
        addOption(options, field);
      } catch (ReflectiveOperationException e) {
        throw new Error("reflecting option from field '" + field + "' failed for " + options, e);
      }
    }

    static String generateName(String name) {
      boolean hasUppercase = !name.equals(name.toLowerCase());
      StringBuilder defaultName = new StringBuilder();
      if (hasUppercase) {
        defaultName.append("--");
        name.chars()
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
        defaultName.append(name.replace('_', '-'));
      }
      return defaultName.toString();
    }

    /** Dump command properties using the provided string consumer. */
    Command dump(Consumer<String> consumer) {
      ListIterator<String> iterator = arguments.listIterator();
      consumer.accept(executable);
      while (iterator.hasNext()) {
        String argument = iterator.next();
        int nextIndex = iterator.nextIndex();
        String indent = nextIndex > dumpOffset || argument.startsWith("-") ? "" : "  ";
        consumer.accept(indent + argument);
        if (nextIndex > dumpLimit) {
          int last = arguments.size() - 1;
          int diff = last - nextIndex;
          if (diff > 1) {
            consumer.accept(indent + "... [omitted " + diff + " arguments]");
          }
          consumer.accept(indent + arguments.get(last));
          break;
        }
      }
      return this;
    }

    int execute() {
      return execute(System.out, System.err, Collections.emptyMap());
    }

    /** Execute. */
    int execute(PrintStream out, PrintStream err, Map<String, ToolProvider> tools) {
      ToolProvider defaultTool = ToolProvider.findFirst(executable).orElse(null);
      ToolProvider tool = tools.getOrDefault(executable, defaultTool);
      if (tool != null) {
        return tool.run(out, err, toArgumentsArray());
      }
      ProcessBuilder processBuilder = toProcessBuilder();
      processBuilder.redirectErrorStream(true);
      try {
        Process process = processBuilder.start();
        process.getInputStream().transferTo(out);
        return process.waitFor();
      } catch (IOException | InterruptedException e) {
        throw new Error("executing `" + executable + "` failed", e);
      }
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

  /**
   * You can use the javac tool and its options to read Java class and interface definitions and
   * compile them into bytecode and class files.
   *
   * @see <a href="https://docs.oracle.com/javase/9/tools/javac.htm">javac</a>
   */
  class Javac implements JdkTool {
    /** (Legacy) class path. */
    List<Path> classPath = List.of();

    /** (Legacy) locations where to find Java source files. */
    @Option("--source-path")
    transient List<Path> classSourcePaths = List.of();

    /** Generates all debugging information, including local variables. */
    @Option("-g")
    boolean generateAllDebuggingInformation = false;

    /** Output source locations where deprecated APIs are used. */
    boolean deprecation = true;

    /** The destination directory for class files. */
    @Option("-d")
    Path destinationPath = null;

    /** Specify character encoding used by source files. */
    Charset encoding = StandardCharsets.UTF_8;

    /** Terminate compilation if warnings occur. */
    @Option("-Werror")
    boolean failOnWarnings = true;

    /** Specify where to find application modules. */
    List<Path> modulePath = List.of();

    /** Where to find input source files for multiple modules. */
    List<Path> moduleSourcePath = List.of();

    /** Generate metadata for reflection on method parameters. */
    boolean parameters = true;

    /** Output messages about what the compiler is doing. */
    boolean verbose = false;
  }

  /**
   * You can use the java command to launch a Java application.
   *
   * @see <a href="https://docs.oracle.com/javase/9/tools/java.htm">java</a>
   */
  class Java implements JdkTool {
    /**
     * Creates the VM but doesn't execute the main method.
     *
     * <p>This {@code --dry-run} option may be useful for validating the command-line options such
     * as the module system configuration.
     */
    boolean dryRun = false;

    /** Where to find application modules. */
    List<Path> modulePath = List.of();

    /** Initial module to resolve and the name of the main class to execute. */
    @Option("--module")
    String module = null;
  }

  /**
   * You use the javadoc tool and options to generate HTML pages of API documentation from Java
   * source files.
   *
   * @see <a href="https://docs.oracle.com/javase/9/tools/javadoc.htm">javadoc</a>
   */
  class Javadoc implements JdkTool {
    /** Shuts off messages so that only the warnings and errors appear. */
    boolean quiet = true;
  }

  /**
   * You can use the jar command to create an archive for classes and resources, and manipulate or
   * restore individual classes or resources from an archive.
   *
   * @see <a href="https://docs.oracle.com/javase/9/tools/jar.htm">jar</a>
   */
  class Jar implements JdkTool {
    /** Specify the operation mode for the jar command. */
    @Option("")
    String mode = "--create";

    /** Specifies the archive file name. */
    @Option("--file")
    Path file = Paths.get("out.jar");

    /** Specifies the application entry point for stand-alone applications. */
    String mainClass = null;

    /** Specifies the module version, when creating a modular JAR file. */
    String moduleVersion = null;

    /** Stores without using ZIP compression. */
    boolean noCompress = false;

    /** Sends or prints verbose output to standard output. */
    @Option("--verbose")
    boolean verbose = false;

    /** Changes to the specified directory and includes the files at the end of the command. */
    @Option("-C")
    Path path = null;
  }

  /**
   * You use the jdeps command to launch the Java class dependency analyzer.
   *
   * @see <a href="https://docs.oracle.com/javase/9/tools/jdeps.htm">jdeps</a>
   */
  class Jdeps implements JdkTool {
    /** Specifies where to find class files. */
    List<Path> classpath = List.of();

    /** Recursively traverses all dependencies. */
    boolean recursive = true;

    /** Finds class-level dependencies in JDK internal APIs. */
    boolean jdkInternals = false;

    /** Shows profile or the file containing a package. */
    boolean profile = false;

    /** Restricts analysis to APIs, like deps from the signature of public and protected members. */
    boolean apionly = false;

    /** Prints dependency summary only. */
    boolean summary = false;

    /** Prints all class-level dependencies. */
    boolean verbose = false;
  }

  /**
   * You can use the jlink tool to assemble and optimize a set of modules and their dependencies
   * into a custom runtime image.
   *
   * @see <a href="https://docs.oracle.com/javase/9/tools/jlink.htm">jlink</a>
   */
  class Jlink implements JdkTool {
    /** Where to find application modules. */
    List<Path> modulePath = List.of();

    /** The directory that contains the resulting runtime image. */
    @Option("--output")
    Path output = null;
  }

  static Command command(String executable, Object... arguments) {
    return new Command(executable).addAll(List.of(arguments));
  }

  static int execute(String executable, Object... arguments) {
    return command(executable, arguments).execute();
  }

  default String name() {
    return getClass().getSimpleName().toLowerCase();
  }

  default Command toCommand() {
    Command command = command(name());
    command.addAllOptions(this);
    return command;
  }
}

/** Common utilities and helpers. */
interface JdkUtil {

  /** Download the resource specified by its URI to the target directory. */
  static Path download(URI uri, Path targetDirectory) throws IOException {
    return download(uri, targetDirectory, fileName(uri), path -> true);
  }

  /** Download the resource from URI to the target directory using the provided file name. */
  static Path download(URI uri, Path directory, String fileName, Predicate<Path> skip)
      throws IOException {
    URL url = uri.toURL();
    Files.createDirectories(directory);
    Path target = directory.resolve(fileName);
    if (Boolean.getBoolean("bach.offline")) {
      if (Files.exists(target)) {
        return target;
      }
      throw new Error("offline mode is active -- missing file " + target);
    }
    URLConnection urlConnection = url.openConnection();
    FileTime urlLastModifiedTime = FileTime.fromMillis(urlConnection.getLastModified());
    if (urlLastModifiedTime.toMillis() == 0) {
      throw new IOException("last-modified header field not available");
    }
    // TODO log("downloading `%s` [%s]...", url, urlLastModifiedTime);
    if (Files.exists(target)) {
      if (Files.getLastModifiedTime(target).equals(urlLastModifiedTime)) {
        if (Files.size(target) == urlConnection.getContentLengthLong()) {
          if (skip.test(target)) {
            // TODO log("skipped, using `%s`", target);
            return target;
          }
        }
      }
      Files.delete(target);
    }
    // TODO log("transferring `%s`...", uri);
    try (InputStream sourceStream = url.openStream();
        OutputStream targetStream = Files.newOutputStream(target)) {
      sourceStream.transferTo(targetStream);
    }
    Files.setLastModifiedTime(target, urlLastModifiedTime);
    // TODO log("stored `%s` [%s]", target, urlLastModifiedTime);
    return target;
  }

  /** Extract the file name from the uri. */
  static String fileName(URI uri) {
    String urlString = uri.getPath();
    int begin = urlString.lastIndexOf('/') + 1;
    return urlString.substring(begin).split("\\?")[0].split("#")[0];
  }

  static Stream<Path> findDirectories(Path root) {
    try {
      return Files.find(root, 1, (path, attr) -> Files.isDirectory(path))
          .filter(path -> !root.equals(path));
    } catch (Exception e) {
      throw new Error("should not happen", e);
    }
  }

  static Stream<String> findDirectoryNames(Path root) {
    return findDirectories(root).map(root::relativize).map(Path::toString);
  }

  /** Return {@code true} if the path points to a canonical Java archive file. */
  static boolean isJarFile(Path path) {
    if (Files.isRegularFile(path)) {
      return path.getFileName().toString().endsWith(".jar");
    }
    return false;
  }

  /** Return {@code true} if the path points to a canonical Java compilation unit file. */
  static boolean isJavaFile(Path path) {
    if (Files.isRegularFile(path)) {
      String unit = path.getFileName().toString();
      if (unit.endsWith(".java")) {
        return unit.indexOf('.') == unit.length() - 5; // single dot in filename
      }
    }
    return false;
  }

  static void treeDelete(Path root) throws IOException {
    treeDelete(root, path -> true);
  }

  static void treeDelete(Path root, Predicate<Path> filter) throws IOException {
    if (Files.notExists(root)) {
      return;
    }
    try (Stream<Path> stream = Files.walk(root)) {

      List<Path> paths =
          stream
              .filter(p -> !root.equals(p))
              .filter(filter)
              .sorted((p, q) -> -p.compareTo(q))
              .collect(Collectors.toList());
      for (Path path : paths) {
        Files.deleteIfExists(path);
      }
    }
  }
}

class Bach {}
