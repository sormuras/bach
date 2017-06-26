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
import java.util.function.*;
import java.util.logging.*;
import java.util.spi.*;
import java.util.stream.*;

/**
 * Bach - Use jshell to build your modular project.
 *
 * @see <a href="https://github.com/sormuras/bach">bach</a>
 * @see <a href="https://docs.oracle.com/javase/9/tools/jshell.htm">jshell</a>
 */
@SuppressWarnings({"SimplifiableIfStatement", "WeakerAccess"})
class Bach {

  List<String> repositories =
      new ArrayList<>(
          List.of(
              "https://oss.sonatype.org/content/repositories/snapshots",
              "http://repo1.maven.org/maven2",
              "https://jcenter.bintray.com",
              "https://jitpack.io"));

  /** Standard error stream. */
  PrintStream streamErr = System.err;

  /** Standard out stream. */
  PrintStream streamOut = System.out;

  /** Map of custom tool providers. */
  Map<String, ToolProvider> tools = new TreeMap<>();

  void call(String tool, Object options, UnaryOperator<Command> operator) {
    Command command = new Command(tool);
    command.addAllOptions(options);
    execute(operator.apply(command));
  }

  void call(String tool, Object... arguments) {
    Command command = new Command(tool);
    Arrays.stream(arguments).forEach(command::add);
    execute(command);
  }

  /** Execute. */
  void execute(Command command) {
    int actual = command.execute();
    if (actual != 0) {
      throw new Error("execution failed with unexpected error code: " + actual);
    }
  }

  /** Use the {@code java} command to launch a Java application. */
  void java(UnaryOperator<JavaOptions> operator) {
    call("java", operator.apply(new JavaOptions()), UnaryOperator.identity());
  }

  /** Use the {@code javac} tool to read compilation units and compile them into bytecode. */
  void javac(UnaryOperator<JavacOptions> operator) {
    JavacOptions options = operator.apply(new JavacOptions());
    UnaryOperator<Command> addAllSourceFiles =
        command -> {
          command.mark(10);
          command.addAll(options.moduleSourcePaths, Util::isJavaFile);
          return command;
        };
    call("javac", options, addAllSourceFiles);
  }

  /** Resolve maven artifact. */
  Path resolve(String group, String artifact, String version, String... more) {
    Path targetDirectory = Paths.get(".bach", "resolved"); // TODO path(Folder.DEPENDENCIES)
    for (String repo : repositories) {
      URI uri = Util.uri(repo, group, artifact, version, more);
      try {
        return Util.download(uri, targetDirectory);
      } catch (IOException e) {
        // continue
      }
    }
    throw new Error("could not resolve artifact: " + group + artifact + version);
  }

  /** Command builder. */
  class Command {

    final List<String> arguments = new ArrayList<>();
    int dumpLimit = Integer.MAX_VALUE;
    int dumpOffset = Integer.MAX_VALUE;
    final String executable;

    Command(String executable) {
      this.executable = executable;
    }

    /** Add single argument composed of joined path names using {@link File#pathSeparator}. */
    void add(Collection<Path> paths) {
      List<String> names = paths.stream().map(Object::toString).collect(Collectors.toList());
      add(String.join(File.pathSeparator, names));
    }

    /** Add single non-null argument. */
    void add(Object argument) {
      arguments.add(argument.toString());
    }

    //    /** Add all stream elements joined to a single argument. */
    //    void add(Stream<?> stream, String separator) {
    //      add(stream.map(Object::toString).collect(Collectors.joining(separator)));
    //    }

    /** Add all files visited by walking specified root paths recursively. */
    void addAll(Collection<Path> roots, Predicate<Path> predicate) {
      roots.forEach(root -> addAll(root, predicate));
    }

    /** Add all files visited by walking specified root path recursively. */
    void addAll(Path root, Predicate<Path> predicate) {
      try (Stream<Path> stream = Files.walk(root).filter(predicate)) {
        stream.forEach(this::add);
      } catch (IOException e) {
        throw new Error("walking path `" + root + "` failed", e);
      }
    }

    /** Add all reflected options. */
    void addAllOptions(Object options) {
      addAllOptions(options, UnaryOperator.identity());
    }

    void addAllOptions(Object options, UnaryOperator<Stream<java.lang.reflect.Field>> operator) {
      Stream<java.lang.reflect.Field> stream =
          Arrays.stream(options.getClass().getDeclaredFields())
              .filter(field -> !field.isSynthetic())
              .filter(field -> !java.lang.reflect.Modifier.isStatic(field.getModifiers()));
      stream = operator.apply(stream);
      stream.forEach(field -> addOptionUnchecked(options, field));
    }

    private void addOption(Object options, Field field) {
      String name = field.getName();
      // custom option visitor method declared?
      try {
        try {
          options.getClass().getDeclaredMethod(name, Command.class).invoke(options, this);
          return;
        } catch (NoSuchMethodException e) {
          // fall-through
        } catch (InvocationTargetException e) {
          throw new Error(e);
        }
        // (guess) option name
        String optionName = "-" + name.replace('_', '-');
        if (field.isAnnotationPresent(OptionName.class)) {
          optionName = field.getAnnotation(OptionName.class).value();
        }
        // is it an omissible boolean flag?
        if (field.getType() == boolean.class) {
          if (field.getBoolean(options)) {
            add(optionName);
          }
          return;
        }
        // as-is
        add(optionName);
        add(Objects.toString(field.get(options)));
      } catch (IllegalAccessException e) {
        throw new Error(e);
      }
    }

    private void addOptionUnchecked(Object options, java.lang.reflect.Field field) {
      try {
        addOption(options, field);
      } catch (Exception e) {
        throw new Error("reflecting options failed for " + options, e);
      }
    }

    /** Dump command properties using the provided string consumer. */
    void dump(Consumer<String> consumer) {
      ListIterator<String> iterator = arguments.listIterator();
      consumer.accept(executable);
      while (iterator.hasNext()) {
        String argument = iterator.next();
        int nextIndex = iterator.nextIndex();
        String indent = nextIndex > dumpOffset || argument.startsWith("-") ? "" : "  ";
        consumer.accept(indent + argument);
        if (nextIndex >= dumpLimit) {
          int last = arguments.size() - 1;
          int diff = last - nextIndex;
          if (diff == 1) {
            consumer.accept(indent + arguments.get(last - 1));
          } else {
            consumer.accept(indent + "... [omitted " + diff + " arguments]");
          }
          consumer.accept(indent + arguments.get(last));
          break;
        }
      }
    }

    /** Execute. */
    int execute() {
      streamOut.println();
      dump(streamOut::println);
      ToolProvider defaultTool = ToolProvider.findFirst(executable).orElse(null);
      ToolProvider tool = tools.getOrDefault(executable, defaultTool);
      if (tool != null) {
        return tool.run(streamOut, streamErr, toArgumentsArray());
      }
      ProcessBuilder processBuilder = toProcessBuilder();
      processBuilder.redirectErrorStream(true);
      try {
        Process process = processBuilder.start();
        process.getInputStream().transferTo(streamOut);
        return process.waitFor();
      } catch (IOException | InterruptedException e) {
        throw new Error("executing `" + executable + "` failed", e);
      }
    }

    /** Set dump offset and limit. */
    void mark(int limit) {
      this.dumpOffset = arguments.size();
      this.dumpLimit = arguments.size() + limit;
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

  /** Use the {@code javac} tool to read compilation units and compile them into bytecode. */
  @SuppressWarnings("unused")
  class JavacOptions {
    /** Output source locations where deprecated APIs are used. */
    boolean deprecation = true;

    /** The destination directory for class files. */
    @OptionName("-d")
    Path destinationPath = Paths.get("target/jshell");

    /** Specify character encoding used by source files. */
    Charset encoding = StandardCharsets.UTF_8;

    /** Terminate compilation if warnings occur. */
    @OptionName("-Werror")
    boolean failOnWarnings = true;

    /** Specify where to find application modules. */
    List<Path> modulePaths = List.of(); // TODO List.of(path(Folder.DEPENDENCIES));

    /** Where to find input source files for multiple modules. */
    List<Path> moduleSourcePaths = List.of(Paths.get("src/main/java"));

    /** Generate metadata for reflection on method parameters. */
    boolean parameters = true;

    /** Output messages about what the compiler is doing. */
    boolean verbose = false; // log.isLevelActive(Level.FINEST);

    void encoding(Command command) {
      if (Charset.defaultCharset().equals(encoding)) {
        return;
      }
      command.add("-encoding");
      command.add(encoding.name());
    }

    void modulePaths(Command command) {
      if (!modulePaths.isEmpty()) {
        command.add("--module-path");
        command.add(modulePaths);
      }
    }

    void moduleSourcePaths(Command command) {
      if (!moduleSourcePaths.isEmpty()) {
        command.add("--module-source-path");
        command.add(moduleSourcePaths);
      }
    }
  }

  /** You can use the {@code java} command to launch a Java application. */
  @SuppressWarnings("unused")
  class JavaOptions {
    /** Where to find application modules. */
    List<Path> modulePaths = List.of(); // TODO List.of(path(Folder.DEPENDENCIES));

    /** Initial module to resolve and the name of the main class to execute. */
    @OptionName("--module")
    String module = null;

    void modulePaths(Command command) {
      if (!modulePaths.isEmpty()) {
        command.add("--module-path");
        command.add(modulePaths);
      }
    }
  }

  /** Command option name annotation. */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  @interface OptionName {
    String value();
  }

  interface Util {

    Logger logger = Logger.getLogger("Bach");

    static Path cleanTree(Path root, boolean keepRoot) {
      return cleanTree(root, keepRoot, path -> true);
    }

    static Path cleanTree(Path root, boolean keepRoot, Predicate<Path> filter) {
      try {
        if (Files.notExists(root)) {
          if (keepRoot) {
            Files.createDirectories(root);
          }
          return root;
        }
        List<Path> paths =
            Files.walk(root)
                .filter(p -> !(keepRoot && root.equals(p)))
                .filter(filter)
                .sorted((p, q) -> -p.compareTo(q))
                .collect(Collectors.toList());
        for (Path path : paths) {
          Files.deleteIfExists(path);
        }
        logger.log(Level.FINE, "deleted tree `" + root + "`");
        return root;
      } catch (IOException e) {
        throw new Error("should not happen", e);
      }
    }

    /** Download the resource specified by its URI to the target directory. */
    static Path download(URI uri, Path targetDirectory) throws IOException {
      return Util.download(uri, targetDirectory, fileName(uri), path -> true);
    }

    /** Download the resource from URI to the target directory using the provided file name. */
    static Path download(URI uri, Path directory, String fileName, Predicate<Path> skip)
        throws IOException {
      URL url = uri.toURL();
      Files.createDirectories(directory);
      Path target = directory.resolve(fileName);
      URLConnection urlConnection = url.openConnection();
      FileTime urlLastModifiedTime = FileTime.fromMillis(urlConnection.getLastModified());
      if (Files.exists(target)) {
        if (Files.getLastModifiedTime(target).equals(urlLastModifiedTime)) {
          if (Files.size(target) == urlConnection.getContentLengthLong()) {
            if (skip.test(target)) {
              logger.log(Level.FINE, "skipped, using `" + target + "`");
              return target;
            }
          }
        }
        Files.delete(target);
      }
      logger.log(Level.FINE, "transferring `" + uri + "`...");
      try (InputStream sourceStream = url.openStream();
          OutputStream targetStream = Files.newOutputStream(target)) {
        sourceStream.transferTo(targetStream);
      }
      Files.setLastModifiedTime(target, urlLastModifiedTime);
      logger.log(Level.FINE, "stored `" + target + "` [" + urlLastModifiedTime + "]");
      return target;
    }

    /** Extract the file name from the uri. */
    static String fileName(URI uri) {
      String urlString = uri.getPath();
      int begin = urlString.lastIndexOf('/') + 1;
      return urlString.substring(begin).split("\\?")[0].split("#")[0];
    }

    /** Return {@code true} if the string is {@code null} or empty. */
    static boolean isBlank(String string) {
      return string == null || string.isEmpty() || string.trim().isEmpty();
    }

    /** Return {@code true} if the path points to a canonical jar file. */
    static boolean isJarFile(Path path) {
      if (Files.isRegularFile(path)) {
        return path.getFileName().toString().endsWith(".jar");
      }
      return false;
    }

    /** Return {@code true} if the path points to a canonical Java compilation unit. */
    static boolean isJavaFile(Path path) {
      if (Files.isRegularFile(path)) {
        String name = path.getFileName().toString();
        if (name.endsWith(".java")) {
          return name.chars().filter(c -> c == '.').count() == 1;
        }
      }
      return false;
    }

    /** Get uri for specified maven coordinates. */
    static URI uri(String repo, String group, String artifact, String version, String... args) {
      group = group.replace('.', '/');
      String classifier = args.length < 1 ? "" : args[0];
      String kind = args.length < 2 ? "jar" : args[1];
      String path = artifact + '/' + version;
      String versifier = isBlank(classifier) ? version : version + '-' + classifier;
      String file = artifact + '-' + versifier + '.' + kind;
      if (version.endsWith("SNAPSHOT")) {
        try {
          URI metaUri = URI.create(repo + '/' + group + '/' + path + '/' + "maven-metadata.xml");
          try (InputStream sourceStream = metaUri.toURL().openStream();
              ByteArrayOutputStream targetStream = new ByteArrayOutputStream()) {
            sourceStream.transferTo(targetStream);
            String meta = targetStream.toString("UTF-8");
            UnaryOperator<String> extract =
                key -> {
                  int begin = meta.indexOf(key) + key.length();
                  int end = meta.indexOf('<', begin);
                  return meta.substring(begin, end).trim();
                };
            String timestamp = extract.apply("<timestamp>");
            String buildNumber = extract.apply("<buildNumber>");
            file = file.replace("SNAPSHOT", timestamp + '-' + buildNumber);
          }
        } catch (IOException e) {
          // fall-through and return with "SNAPSHOT" literal
        }
      }
      return URI.create(repo + '/' + group + '/' + path + '/' + file);
    }
  }
}
