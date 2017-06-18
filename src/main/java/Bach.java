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
import java.net.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.function.*;
import java.util.logging.*;
import java.util.spi.*;
import java.util.stream.*;

/**
 * Java Shell Builder - Use jshell to build your modular project.
 *
 * @see <a href="https://docs.oracle.com/javase/9/tools/jshell.htm">jshell</a>
 */
@SuppressWarnings({"SimplifiableIfStatement", "WeakerAccess"})
public interface Bach {

  default void build() {
    long start = System.currentTimeMillis();
    Util.log.fine("building...");
    clean();
    format();
    compile();
    test();
    link();
    Util.log.info(() -> "finished after " + (System.currentTimeMillis() - start) + " ms");
  }

  /** Create and execute command. */
  int call(String executable, Object... arguments);

  default void clean() {
    Util.log.warning("not implemented, yet");
  }

  default void compile() {
    Util.log.warning("(javac, javadoc, jar) not implemented, yet");
  }

  Configuration configuration();

  /** Execute command throwing a runtime exception when the exit value is not zero. */
  int execute(Command command);

  default void format() {
    Util.log.warning("not implemented, yet");
  }

  default void link() {
    Util.log.warning("not implemented, yet");
  }

  /** Get path for the specified folder. Same as: {@code configuration().folders().get(folder)} */
  default Path path(Folder folder) {
    return configuration().folders().get(folder);
  }

  /** Resolve named module by downloading its jar artifact from the specified location. */
  default Path resolve(String module, URI uri) {
    Path targetDirectory = path(Folder.DEPENDENCIES);
    return Util.download(uri, targetDirectory, module + ".jar", path -> true);
  }

  default void test() {
    Util.log.warning("not implemented, yet");
  }

  class Builder implements Configuration {
    final Map<Folder, Folder.Location> customFolderLocations = new TreeMap<>();
    Map<Folder, Path> folders = buildFolders(Collections.emptyMap());
    Handler handler = new Util.ConsoleHandler(System.out, Level.ALL);
    Level level = Level.FINE;
    String name = Paths.get(".").toAbsolutePath().normalize().getFileName().toString();
    Map<String, ToolProvider> tools = new TreeMap<>();
    String version = "1.0.0-SNAPSHOT";

    public Bach build() {
      Builder configuration = new Builder();
      configuration.folders = buildFolders(customFolderLocations);
      configuration.tools = Collections.unmodifiableMap(new TreeMap<>(tools));
      configuration.handler = handler;
      configuration.level = level;
      configuration.name = name;
      configuration.version = version;
      Util.setHandler(handler);
      Util.log.setLevel(level);
      return new Default(configuration);
    }

    private Map<Folder, Path> buildFolders(Map<Folder, Folder.Location> locations) {
      Function<Folder, Folder.Location> locator =
          folder -> locations.getOrDefault(folder, folder.location);
      Map<Folder, Path> map = new EnumMap<>(Folder.class);
      for (Folder folder : Folder.values()) {
        Folder.Location location = locator.apply(folder);
        Path path = location.path;
        List<Folder> parents = location.parents;
        if (!parents.isEmpty()) {
          Iterator<Folder> iterator = parents.iterator();
          path = locator.apply(iterator.next()).path;
          while (iterator.hasNext()) {
            path = path.resolve(locator.apply(iterator.next()).path);
          }
          path = path.resolve(location.path);
        }
        map.put(folder, path);
      }
      return Collections.unmodifiableMap(map);
    }

    public Builder folder(Folder folder, Folder.Location location) {
      customFolderLocations.put(folder, location);
      folders = buildFolders(customFolderLocations);
      return this;
    }

    public Builder folder(Folder folder, Path path) {
      return folder(folder, Folder.Location.of(folder.location.parents, path));
    }

    @Override
    public Map<Folder, Path> folders() {
      return folders;
    }

    public Builder handler(Handler handler) {
      this.handler = handler;
      return this;
    }

    public Builder level(Level level) {
      this.level = level;
      return this;
    }

    @Override
    public String name() {
      return name;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    @Override
    public Map<String, ToolProvider> tools() {
      return tools;
    }

    public Builder tool(ToolProvider toolProvider) {
      tools.put(toolProvider.name(), toolProvider);
      return this;
    }

    @Override
    public String version() {
      return version;
    }

    public Builder version(String version) {
      this.version = version;
      return this;
    }
  }

  class Command {
    final List<String> arguments = new ArrayList<>();
    int dumpLimit = Integer.MAX_VALUE;
    int dumpOffset = Integer.MAX_VALUE;
    final String executable;
    final Logger logger;

    public Command(Path executable) {
      this(executable.toString());
    }

    public Command(String executable) {
      this.executable = executable;
      this.logger = Logger.getLogger("Bach");
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

    /** Add all specified folders joined to a single argument using {@link File#pathSeparator}. */
    public Command add(Function<Bach.Folder, Path> mapper, Bach.Folder... folders) {
      return add(Arrays.stream(folders).map(mapper), File.pathSeparator);
    }

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
        throw new Error("walking path `" + path + "` failed", e);
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
      if (field.isAnnotationPresent(Bach.Util.OptionName.class)) {
        optionName = field.getAnnotation(Bach.Util.OptionName.class).value();
      }
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
    void dump(Level level) {
      if (logger.isLoggable(level)) {
        dump(message -> logger.log(level, message));
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

  interface Configuration {
    default void dump(Consumer<String> consumer) {
      consumer.accept(name() + " " + version());
      consumer.accept("  " + folders());
    }

    Map<Folder, Path> folders();

    String name();

    Map<String, ToolProvider> tools();

    String version();
  }

  class Default implements Bach {

    final Configuration configuration;
    final PrintStream streamErr = System.err;
    final PrintStream streamOut = System.out;

    Default(Configuration configuration) {
      this.configuration = configuration;
      configuration.dump(Util.log::config);
      Util.log.info("initialized");
    }

    @Override
    public int call(String executable, Object... arguments) {
      return execute(new Command(executable).addAll(arguments));
    }

    @Override
    public Configuration configuration() {
      return configuration;
    }

    /** Execute command throwing a runtime exception when the exit value is not zero. */
    public int execute(Command command) {
      return execute(command, Util::exitValueChecker);
    }

    /** Execute command with supplied exit value checker. */
    int execute(Command command, Consumer<Integer> exitValueChecker) {
      Level dumpLevel = Level.FINE;
      String executable = command.executable;
      long start = System.currentTimeMillis();
      Integer exitValue = null;
      ToolProvider providedTool = configuration.tools().get(executable);
      if (providedTool != null) {
        Util.log.fine(() -> "executing provided `" + executable + "` tool...");
        command.dump(dumpLevel);
        exitValue = providedTool.run(streamOut, streamErr, command.toArgumentsArray());
      }
      if (exitValue == null) {
        Optional<ToolProvider> tool = ToolProvider.findFirst(executable);
        if (tool.isPresent()) {
          Util.log.fine(() -> "executing loaded `" + executable + "` tool...");
          command.dump(dumpLevel);
          exitValue = tool.get().run(streamOut, streamErr, command.toArgumentsArray());
        }
      }
      if (exitValue == null) {
        Util.log.fine(() -> "executing external `" + executable + "` tool...");
        command.dump(dumpLevel);
        ProcessBuilder processBuilder = command.toProcessBuilder().redirectErrorStream(true);
        try {
          Process process = processBuilder.start();
          process.getInputStream().transferTo(streamOut);
          exitValue = process.waitFor();
        } catch (Exception e) {
          if (!Util.log.isLoggable(dumpLevel)) {
            command.dump(Level.SEVERE);
          }
          throw new Error("executing `" + executable + "` failed", e);
        }
      }
      long duration = System.currentTimeMillis() - start;
      Util.log.info(() -> executable + " finished after " + duration + " ms");
      exitValueChecker.accept(exitValue);
      return exitValue;
    }
  }

  enum Folder {
    JDK_HOME(Location.of(Util.findJdkHome())),
    AUXILIARY(Location.of(Paths.get(".bach"))),
    TOOLS(Location.of(List.of(AUXILIARY), Paths.get("tools"))),
    DEPENDENCIES(Location.of(List.of(AUXILIARY), Paths.get("dependencies")));

    public static class Location {
      final List<Folder> parents;
      final Path path;

      private Location(List<Folder> parents, Path path) {
        this.path = path;
        this.parents = parents;
      }

      public static Location of(List<Folder> parents, Path path) {
        return new Location(parents, path);
      }

      public static Location of(Path path) {
        return new Location(Collections.emptyList(), path);
      }
    }

    final Location location;

    Folder(Location location) {
      this.location = location;
    }
  }

  interface Util {

    Logger log = Logger.getLogger("Bach");

    class ConsoleHandler extends StreamHandler {

      public ConsoleHandler(OutputStream stream, Level level) {
        super(stream, new SingleLineFormatter());
        setLevel(level);
      }

      @Override
      public void publish(LogRecord record) {
        super.publish(record);
        flush();
      }

      @Override
      public void close() {
        flush();
      }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface OptionName {
      String value();
    }

    class SingleLineFormatter extends java.util.logging.Formatter {

      @SuppressWarnings("SpellCheckingInspection")
      private final DateTimeFormatter instantFormatter =
          DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss:SSS").withZone(ZoneId.systemDefault());

      @Override
      public String format(LogRecord record) {
        StringBuilder builder = new StringBuilder();
        if (log.getLevel().intValue() < Level.FINE.intValue()) {
          builder.append(instantFormatter.format(record.getInstant()));
          builder.append(' ');
          builder.append(record.getSourceClassName());
          builder.append(' ');
          builder.append(record.getSourceMethodName());
          builder.append(' ');
        }
        builder.append(formatMessage(record));
        builder.append(System.lineSeparator());
        if (record.getThrown() == null) {
          return builder.toString();
        }
        builder.append(System.lineSeparator());
        builder.append('-');
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        record.getThrown().printStackTrace(pw);
        pw.close();
        builder.append(sw.getBuffer());
        return builder.toString();
      }
    }

    /** Throw an {@link Error} when exit value is not zero. */
    static void exitValueChecker(int value) {
      if (value == 0) {
        return;
      }
      throw new Error("exit value " + value + " indicates an error");
    }

    /** Maven uri for jar artifact at {@code http://central.maven.org/maven2} repository. */
    static URI maven(String group, String artifact, String version) {
      return maven("http://central.maven.org/maven2", group, artifact, version, "jar");
    }

    /** Maven uri for specified coordinates. */
    static URI maven(String repo, String group, String artifact, String version, String kind) {
      String path = artifact + '/' + version + '/' + artifact + '-' + version + '.' + kind;
      return URI.create(repo + '/' + group.replace('.', '/') + '/' + path);
    }

    /** Download the resource specified by its URI to the target directory. */
    static Path download(URI uri, Path targetDirectory) {
      return Util.download(uri, targetDirectory, extractFileName(uri), path -> true);
    }

    /** Download the resource from URI to the target directory using the provided file name. */
    static Path download(URI uri, Path directory, String fileName, Predicate<Path> skip) {
      try {
        URL url = uri.toURL();
        Files.createDirectories(directory);
        Path target = directory.resolve(fileName);
        URLConnection urlConnection = url.openConnection();
        FileTime urlLastModifiedTime = FileTime.fromMillis(urlConnection.getLastModified());
        if (Files.exists(target)) {
          if (Files.getLastModifiedTime(target).equals(urlLastModifiedTime)) {
            if (Files.size(target) == urlConnection.getContentLengthLong()) {
              if (skip.test(target)) {
                log.fine(() -> "skipped, using `" + target + "`");
                return target;
              }
            }
          }
          Files.delete(target);
        }
        log.fine(() -> "transferring `" + uri + "`...");
        try (InputStream sourceStream = url.openStream();
            OutputStream targetStream = Files.newOutputStream(target)) {
          sourceStream.transferTo(targetStream);
        }
        Files.setLastModifiedTime(target, urlLastModifiedTime);
        log.info(() -> "stored `" + target + "` [" + urlLastModifiedTime + "]");
        return target;
      } catch (IOException e) {
        throw new Error("should not happen", e);
      }
    }

    /** Extract the file name from the uri. */
    static String extractFileName(URI uri) {
      String urlString = uri.getPath();
      int begin = urlString.lastIndexOf('/') + 1;
      return urlString.substring(begin).split("\\?")[0].split("#")[0];
    }

    /** Return path to JDK installation directory. */
    static Path findJdkHome() {
      Path executable = ProcessHandle.current().info().command().map(Paths::get).orElse(null);
      if (executable != null && executable.getNameCount() > 2) {
        // noinspection ConstantConditions -- count is 3 or higher: "<JDK_HOME>/bin/java[.exe]"
        return executable.getParent().getParent().toAbsolutePath();
      }
      String jdkHome = System.getenv("JDK_HOME");
      if (jdkHome != null) {
        return Paths.get(jdkHome).toAbsolutePath();
      }
      String javaHome = System.getenv("JAVA_HOME");
      if (javaHome != null) {
        return Paths.get(javaHome).toAbsolutePath();
      }
      Path fallback = Paths.get("jdk-" + Runtime.version().major()).toAbsolutePath();
      log.warning("path of JDK not found, using: " + fallback);
      return fallback;
    }

    /** Return {@code true} if the path points to a canonical jar file. */
    static boolean isJarFile(Path path) {
      if (!Files.isRegularFile(path)) {
        return false;
      }
      return path.getFileName().toString().endsWith(".jar");
    }

    /** Return {@code true} if the path points to a canonical Java compilation unit. */
    static boolean isJavaSourceFile(Path path) {
      if (!Files.isRegularFile(path)) {
        return false;
      }
      String name = path.getFileName().toString();
      if (name.chars().filter(c -> c == '.').count() != 1) {
        return false;
      }
      return name.endsWith(".java");
    }

    static void setHandler(Handler handler) {
      for (Handler remove : log.getHandlers()) {
        log.removeHandler(remove);
      }
      if (handler == null) {
        log.setUseParentHandlers(true);
        return;
      }
      log.addHandler(handler);
      log.setUseParentHandlers(false);
    }
  }
}
