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

@SuppressWarnings({"SimplifiableIfStatement", "WeakerAccess", "unused"})
class Bach {

  /** Logger instance. */
  Logger logger = Logger.getLogger("Bach");

  /** Resolver instance. */
  Resolver resolver = new Resolver();

  /** Standard error stream. */
  PrintStream streamErr = System.err;

  /** Standard out stream. */
  PrintStream streamOut = System.out;

  /** Map of custom tool providers. */
  Map<String, ToolProvider> tools = new TreeMap<>();

  /** Execute command expecting an exit code of zero. */
  void call(Command command) {
    int actual = command.execute(streamOut, streamErr, tools);
    if (actual != 0) {
      throw new Error("execution failed with unexpected error code: " + actual);
    }
  }

  /** Execute tool expecting an exit code of zero. */
  void call(String tool, Object options, UnaryOperator<Command> operator) {
    call(operator.apply(new Command(tool).addAllOptions(options)));
  }

  /** Execute tool with arbitrary arguments expecting an exit code of zero. */
  void call(String tool, Object... arguments) {
    Command command = new Command(tool);
    Arrays.stream(arguments).forEach(command::add);
    call(command);
  }

  /** Download the resource specified by its URI to the target directory. */
  Path download(URI uri, Path targetDirectory) throws IOException {
    return download(uri, targetDirectory, resolver.fileName(uri), path -> true);
  }

  /** Download the resource from URI to the target directory using the provided file name. */
  Path download(URI uri, Path directory, String fileName, Predicate<Path> skip) throws IOException {
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

  /** Return {@code true} if the path points to a canonical Java archive file. */
  boolean isJarFile(Path path) {
    if (Files.isRegularFile(path)) {
      return path.getFileName().toString().endsWith(".jar");
    }
    return false;
  }

  /** Return {@code true} if the path points to a canonical Java compilation unit file. */
  boolean isJavaFile(Path path) {
    if (Files.isRegularFile(path)) {
      String unit = path.getFileName().toString();
      if (unit.endsWith(".java")) {
        return unit.indexOf('.') == unit.length() - 5; // single dot in filename
      }
    }
    return false;
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
          command.addAll(options.classSourcePaths, this::isJavaFile);
          command.addAll(options.moduleSourcePaths, this::isJavaFile);
          return command;
        };
    call("javac", options, addAllSourceFiles);
  }

  /** Generate HTML pages of API documentation from Java source files. */
  void javadoc(UnaryOperator<JavadocOptions> operator) {
    call("javadoc", operator.apply(new JavadocOptions()), UnaryOperator.identity());
  }

  /** Create an archive for classes and resources. */
  void jar(UnaryOperator<JarOptions> operator) {
    call("jar", operator.apply(new JarOptions()), UnaryOperator.identity());
  }

  /** Assemble and optimize a set of modules and their dependencies into a custom runtime image. */
  void jlink(UnaryOperator<JlinkOptions> operator) {
    call("jlink", operator.apply(new JlinkOptions()), UnaryOperator.identity());
  }

  /** Resolve maven jar artifact. */
  Path resolve(String group, String artifact, String version) {
    return resolver.resolve(new ResolverArtifact(group, artifact, version));
  }

  /** Command-line executable builder. */
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
        if (field.isAnnotationPresent(CommandOption.class)) {
          optionName = field.getAnnotation(CommandOption.class).value();
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

    List<String> dump() {
      List<String> lines = new ArrayList<>();
      dump(lines::add);
      return lines;
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
          if (diff > 1) {
            consumer.accept(indent + "... [omitted " + diff + " arguments]");
          }
          consumer.accept(indent + arguments.get(last));
          break;
        }
      }
    }

    /** Execute. */
    int execute(PrintStream out, PrintStream err, Map<String, ToolProvider> tools) {
      out.println();
      dump(out::println);
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

  /** Command option annotation. */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  @interface CommandOption {
    String value();
  }

  class JavacOptions {
    /** (Legacy) class path. */
    List<Path> classPaths = List.of();

    /** (Legacy) locations where to find Java source files. */
    transient List<Path> classSourcePaths = List.of();

    /** Output source locations where deprecated APIs are used. */
    boolean deprecation = true;

    /** The destination directory for class files. */
    @CommandOption("-d")
    Path destinationPath = Paths.get("target/bach");

    /** Specify character encoding used by source files. */
    Charset encoding = StandardCharsets.UTF_8;

    /** Terminate compilation if warnings occur. */
    @CommandOption("-Werror")
    boolean failOnWarnings = true;

    /** Specify where to find application modules. */
    List<Path> modulePaths = List.of();

    /** Where to find input source files for multiple modules. */
    List<Path> moduleSourcePaths = List.of();

    /** Generate metadata for reflection on method parameters. */
    boolean parameters = true;

    /** Output messages about what the compiler is doing. */
    boolean verbose = logger.isLoggable(Level.FINEST);

    void classPaths(Command command) {
      if (!classPaths.isEmpty()) {
        command.add("--class-path");
        command.add(classPaths);
      }
    }

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

  class JavaOptions {
    /** Where to find application modules. */
    List<Path> modulePaths = List.of();

    /** Initial module to resolve and the name of the main class to execute. */
    @CommandOption("--module")
    String module = null;

    void modulePaths(Command command) {
      if (!modulePaths.isEmpty()) {
        command.add("--module-path");
        command.add(modulePaths);
      }
    }
  }

  class JavadocOptions {
    /** Shuts off messages so that only the warnings and errors appear. */
    boolean quiet = true;
  }

  class JarOptions {
    /** Stores without using ZIP compression. */
    @CommandOption("--no-compress")
    boolean noCompress = false;

    /** Sends or prints verbose output to standard output. */
    @CommandOption("--verbose")
    boolean verbose = logger.isLoggable(Level.FINEST);
  }

  class JlinkOptions {
    /** Where to find application modules. */
    List<Path> modulePaths = List.of();

    /** The directory that contains the resulting runtime image. */
    @CommandOption("--output")
    Path output = Paths.get("target", "bach", "jlink");

    void modulePaths(Command command) {
      if (!modulePaths.isEmpty()) {
        command.add("--module-path");
        command.add(modulePaths);
      }
    }
  }

  /** Maven artifact resolver. */
  class Resolver {

    List<String> repositories =
        List.of(
            "https://oss.sonatype.org/content/repositories/snapshots",
            "http://repo1.maven.org/maven2",
            "https://jcenter.bintray.com",
            "https://jitpack.io");

    /** Extract the file name from the uri. */
    String fileName(URI uri) {
      String urlString = uri.getPath();
      int begin = urlString.lastIndexOf('/') + 1;
      return urlString.substring(begin).split("\\?")[0].split("#")[0];
    }

    Path resolve(ResolverArtifact ra) {
      Path targetDirectory = Paths.get(".bach", "resolved");
      for (String repo : repositories) {
        URI uri = uri(repo, ra);
        String fileName = fileName(uri);
        // revert local filename with constant version attribute
        if (ra.isSnapshot()) {
          fileName = ra.fileName();
        }
        try {
          return download(uri, targetDirectory, fileName, path -> true);
        } catch (IOException e) {
          // e.printStackTrace();
        }
      }
      throw new Error("could not resolve artifact: " + ra);
    }

    /** Create uri for specified maven coordinates. */
    URI uri(String repo, ResolverArtifact ra) {
      ra.group = ra.group.replace('.', '/');
      String path = ra.artifact + '/' + ra.version;
      String file = ra.fileName();
      if (ra.isSnapshot()) {
        try {
          URI metaUri = URI.create(repo + '/' + ra.group + '/' + path + '/' + "maven-metadata.xml");
          try (InputStream sourceStream = metaUri.toURL().openStream();
              ByteArrayOutputStream targetStream = new ByteArrayOutputStream()) {
            sourceStream.transferTo(targetStream);
            String meta = targetStream.toString("UTF-8");
            UnaryOperator<String> extractor =
                key -> {
                  int begin = meta.indexOf(key) + key.length();
                  int end = meta.indexOf('<', begin);
                  return meta.substring(begin, end).trim();
                };
            String timestamp = extractor.apply("<timestamp>");
            String buildNumber = extractor.apply("<buildNumber>");
            file = file.replace("SNAPSHOT", timestamp + '-' + buildNumber);
          }
        } catch (IOException e) {
          // fall-through and return with "SNAPSHOT" literal
        }
      }
      return URI.create(repo + '/' + ra.group + '/' + path + '/' + file);
    }
  }

  class ResolverArtifact {
    String group;
    String artifact;
    String version;
    String classifier;
    String kind;

    ResolverArtifact(String group, String artifact, String version) {
      this.group = group;
      this.artifact = artifact;
      this.version = version;
      this.classifier = "";
      this.kind = "jar";
    }

    String fileName() {
      String versifier = classifier.isEmpty() ? version : version + '-' + classifier;
      return artifact + '-' + versifier + '.' + kind;
    }

    boolean isSnapshot() {
      return version.endsWith("SNAPSHOT");
    }
  }
}
