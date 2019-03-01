import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.WARNING;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Java Shell Builder. */
@SuppressWarnings("WeakerAccess")
class Bach {

  /** Version is either {@code master} or {@link Runtime.Version#parse(String)}-compatible. */
  static final String VERSION = "master";

  /** Convenient short-cut to {@code "user.home"} as a path. */
  static final Path USER_HOME = Path.of(System.getProperty("user.home"));

  /** Convenient short-cut to {@code "user.dir"} as a path. */
  static final Path USER_PATH = Path.of(System.getProperty("user.dir"));

  /** Main program. */
  public static void main(String... args) {
    zero("Bach.java", main(List.of(args)));
  }

  /** Main program. */
  static int main(List<String> args) {
    var tasks = new ArrayList<Task>();
    if (args.isEmpty()) {
      tasks.add(Action.BANNER);
      tasks.add(Action.COMPILE);
      tasks.add(Action.TEST);
      tasks.add(Action.DOCUMENT);
      tasks.add(Action.PACK);
      tasks.add(Action.LINK);
    } else {
      var arguments = new ArrayDeque<>(args);
      while (!arguments.isEmpty()) {
        var argument = arguments.removeFirst();
        if (argument.equalsIgnoreCase("tool")) {
          if (arguments.isEmpty()) {
            throw new Error("No name supplied for tool action!");
          }
          var name = arguments.removeFirst();
          tasks.add(Task.of(name, arguments));
          break;
        }
        var name = argument.toUpperCase();
        var task = Action.valueOf(name);
        tasks.add(task);
      }
    }
    return new Bach().run(tasks);
  }

  static void zero(String context, int code) {
    if (code != 0) {
      throw new Error(context + " failed -- expected exit code of 0, but got: " + code);
    }
  }

  /** Managed properties loaded from {@code ${base}/bach.properties} file. */
  final Properties properties = Property.loadProperties(USER_PATH.resolve("bach.properties"));

  /** Offline mode. */
  boolean offline = Boolean.parseBoolean(get(Property.OFFLINE));

  /** Log level. */
  Level level = System.Logger.Level.valueOf(System.getProperty("bach.level", "INFO"));

  PrintStream err = System.err;
  PrintStream out = System.out;

  /** Display banner. */
  void banner() {
    out.println(
        ""
            + "    ___      ___      ___      ___   \n"
            + "   /\\  \\    /\\  \\    /\\  \\    /\\__\\\n"
            + "  /::\\  \\  /::\\  \\  /::\\  \\  /:/__/_\n"
            + " /::\\:\\__\\/::\\:\\__\\/:/\\:\\__\\/::\\/\\__\\\n"
            + " \\:\\::/  /\\/\\::/  /\\:\\ \\/__/\\/\\::/  /\n"
            + "  \\::/  /   /:/  /  \\:\\__\\    /:/  /\n"
            + "   \\/__/    \\/__/    \\/__/    \\/__/.java"
            + " "
            + VERSION
            + "\n");
  }

  /** Compile main and test sources. */
  void compile() {}

  Path download(Path destination, URI uri) throws Exception {
    log(DEBUG, String.format("download(%s, %s)", destination, uri));
    var name = Path.of(uri.getPath()).getFileName().toString();
    var target = destination.resolve(name);
    if (Files.exists(target)) {
      log(DEBUG, String.format("download skipped, using %s", target.toUri()));
      return target;
    }
    if (offline) {
      throw new IllegalStateException("offline and target does not exist: " + target.toUri());
    }
    log(INFO, String.format("downloading %s to %s <- %s", name, destination, uri));
    try (var stream = uri.toURL().openStream()) {
      Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
    }
    log(INFO, String.format("Downloaded %s to %s <- %s", name, destination, uri));
    return target;
  }

  /** Unzip. */
  Path extract(Path zip) throws Exception {
    var jar = ToolProvider.findFirst("jar").orElseThrow();
    var listing = new StringWriter();
    var printWriter = new PrintWriter(listing);
    jar.run(printWriter, printWriter, "--list", "--file", zip.toString());
    // TODO Find better way to extract root folder name...
    var root = Path.of(listing.toString().split("\\R")[0]);
    var destination = zip.getParent();
    var home = destination.resolve(root);
    if (Files.notExists(home)) {
      if (destination.equals(USER_PATH)) {
        jar.run(out, err, "--extract", "--file", zip.toString());
      } else {
        new ProcessBuilder("jar", "--extract", "--file", zip.toString())
            .inheritIO()
            .directory(destination.toFile())
            .start()
            .waitFor();
      }
    }
    return home.normalize().toAbsolutePath();
  }

  /** Format all Java source units in user's current working directory. */
  void format() {
    format(true, Path.of(""));
  }

  /** Format in-place or just check all Java source units in supplied roots. */
  void format(boolean replace, Path... roots) {
    new Tool.Format(replace, List.of(roots)).execute(this);
  }

  /** Get value for the supplied property, using its key and default value. */
  String get(Property property) {
    return get(property.key, property.defaultValue);
  }

  /** Get value for the supplied property key. */
  String get(String key, String defaultValue) {
    var value = System.getProperty(key);
    if (value != null) {
      return value;
    }
    return properties.getProperty(key, defaultValue);
  }

  /** Jar main binaries, main sources and javadoc. */
  void pack() {}

  /** Run Javadoc on all Java source units. */
  void document() {}

  void help() {
    out.println();
    for (var action : Bach.Action.values()) {
      var name = action.name().toLowerCase();
      var text = String.join('\n' + " ".repeat(14), action.description);
      out.println(String.format(" %-9s    %s", name, text));
    }
    out.println();
  }

  /** Create a custom modular run-time image. */
  void link() {}

  void log(Level level, String message) {
    if (level.getSeverity() < this.level.getSeverity()) {
      return;
    }
    out.printf("%s %s %n", level, message);
  }

  /** Run supplied tasks. */
  int run(List<Task> tasks) {
    try {
      log(DEBUG, String.format("Running %d task(s)...", tasks.size()));
      for (var task : tasks) {
        log(DEBUG, String.format(">> %s", task));
        task.execute(this);
        log(DEBUG, String.format("<< %s", task));
      }
      return 0;
    } catch (RuntimeException exception) {
      exception.printStackTrace();
      return 1;
    }
  }

  /** Run named tool. */
  void run(String name, String... args) {
    log(DEBUG, String.format("run(%s, %s)", name, List.of(args)));
    var toolProvider = ToolProvider.findFirst(name);
    if (toolProvider.isPresent()) {
      var tool = toolProvider.get();
      log(DEBUG, "Running provided tool in-process: " + tool);
      zero(name, tool.run(out, err, args));
      return;
    }
    try {
      var command = new ArrayList<String>();
      command.add(name);
      command.addAll(List.of(args));
      var process = new ProcessBuilder(command).inheritIO().start();
      log(DEBUG, "Running tool in a new process: " + process);
      zero(process.toString(), process.waitFor());
      process.destroy();
    } catch (Exception e) {
      throw new Error("Running tool " + name + " failed!", e);
    }
  }

  /** Copy all files and directories from source to target directory. */
  void treeCopy(Path source, Path target) throws Exception {
    treeCopy(source, target, __ -> true);
  }

  /** Copy selected files and directories from source to target directory. */
  void treeCopy(Path source, Path target, Predicate<Path> filter) throws Exception {
    // debug("treeCopy(source:`%s`, target:`%s`)%n", source, target);
    if (!Files.exists(source)) {
      throw new IllegalArgumentException("source must exist: " + source);
    }
    if (!Files.isDirectory(source)) {
      throw new IllegalArgumentException("source must be a directory: " + source);
    }
    if (Files.exists(target)) {
      if (!Files.isDirectory(target)) {
        throw new IllegalArgumentException("target must be a directory: " + target);
      }
      if (target.equals(source)) {
        return;
      }
      if (target.startsWith(source)) {
        // copy "a/" to "a/b/"...
        throw new IllegalArgumentException("target must not a child of source");
      }
    }
    try (var stream = Files.walk(source).sorted()) {
      int counter = 0;
      var paths = stream.collect(Collectors.toList());
      for (var path : paths) {
        var destination = target.resolve(source.relativize(path));
        if (Files.isDirectory(path)) {
          Files.createDirectories(destination);
          continue;
        }
        if (filter.test(path)) {
          Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
          counter++;
        }
      }
      log(DEBUG, String.format("Copied %d file(s) of %d elements.", counter, paths.size()));
    }
  }

  /** Delete all files and directories from the root directory. */
  void treeDelete(Path root) throws Exception {
    treeDelete(root, __ -> true);
  }

  /** Delete selected files and directories from the root directory. */
  void treeDelete(Path root, Predicate<Path> filter) throws Exception {
    // trivial case: delete existing empty directory or single file
    try {
      Files.deleteIfExists(root);
      return;
    } catch (DirectoryNotEmptyException ignored) {
      // fall-through
    }
    // default case: walk the tree...
    try (var stream = Files.walk(root)) {
      var selected = stream.filter(filter).sorted((p, q) -> -p.compareTo(q));
      for (var path : selected.collect(Collectors.toList())) {
        Files.deleteIfExists(path);
      }
    }
  }

  /** Walk directory tree structure. */
  void treeWalk(Path root, Consumer<String> out) {
    try (var stream = Files.walk(root)) {
      stream
          .map(root::relativize)
          .map(path -> path.toString().replace('\\', '/'))
          .sorted()
          .filter(Predicate.not(String::isEmpty))
          .forEach(out);
    } catch (Exception e) {
      throw new Error("Walking tree failed: " + root, e);
    }
  }

  void version() {
    out.println("Bach.java " + VERSION);
  }

  /** Default task implementation. */
  enum Action implements Task {
    BANNER(Bach::banner, "Display a fancy multi-line ASCII art banner"),
    COMPILE(Bach::compile, "Compile main and test sources"),
    DOCUMENT(Bach::document, "Generate javadoc"),
    FORMAT(Bach::format, "Format all Java sources"),
    HELP(Bach::help, "Print this help screen"),
    LINK(Bach::link, "Create a custom modular run-time image"),
    PACK(Bach::pack, "Create jars from main binaries, sources and javadoc"),
    TEST("Launch JUnit Platform"),
    TOOL(
        "Run named tool consuming all remaining arguments",
        "  tool <name> <args...>",
        "  tool java --show-version Program.java"),
    VERSION(Bach::version, "Display version information and continue");

    @Override
    public void execute(Bach bach) {
      if (task == null) {
        bach.log(WARNING, this + " not linked, yet");
        return;
      }
      task.execute(bach);
    }

    final Task task;
    final String[] description;

    Action(String... description) {
      this(null, description);
    }

    Action(Task task, String... description) {
      this.task = task;
      this.description = description;
    }
  }

  /** Command line builder. */
  static class Command implements Task {

    /** Test supplied path for pointing to a Java source unit file. */
    static boolean isJavaFile(Path path) {
      if (Files.isRegularFile(path)) {
        var name = path.getFileName().toString();
        if (name.endsWith(".java")) {
          return name.indexOf('.') == name.length() - 5; // single dot in filename
        }
      }
      return false;
    }

    final String name;
    final List<String> arguments;

    Command(String name, Object... initialArguments) {
      this.name = name;
      this.arguments = new ArrayList<>();
      addAll(initialArguments);
    }

    /** Add single non-null argument. */
    Command add(Object argument) {
      arguments.add(argument.toString());
      return this;
    }

    /** Add single argument composed of joined path names using {@link File#pathSeparator}. */
    Command add(Collection<Path> paths) {
      return add(paths.stream(), File.pathSeparator);
    }

    /** Add single argument composed of all stream elements joined by specified separator. */
    Command add(Stream<?> stream, String separator) {
      return add(stream.map(Object::toString).collect(Collectors.joining(separator)));
    }

    /** Add all arguments by invoking {@link #add(Object)} for each element. */
    Command addAll(Object... arguments) {
      for (var argument : arguments) {
        add(argument);
      }
      return this;
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
      } catch (Exception e) {
        throw new Error("walking path `" + root + "` failed", e);
      }
      return this;
    }

    /** Add all files visited by walking specified root paths recursively. */
    Command addAll(Collection<Path> roots, Predicate<Path> predicate) {
      for (var root : roots) {
        if (Files.notExists(root)) {
          continue;
        }
        addAll(root, predicate);
      }
      return this;
    }

    /** Add all {@code .java} source files by walking specified root paths recursively. */
    Command addAllJavaFiles(Collection<Path> roots) {
      return addAll(roots, Command::isJavaFile);
    }

    @Override
    public void execute(Bach bach) {
      bach.run(name, arguments.toArray(String[]::new));
    }
  }

  /** Constants with default values. */
  enum Property {
    /** Default Maven repository used for artifact resolution. */
    MAVEN_REPOSITORY("https://repo1.maven.org/maven2"),

    /** Offline mode. */
    OFFLINE("false"),

    /** Cache of binary tools. */
    PATH_CACHE_TOOLS(".bach/tools"),

    /** Cache of resolved modules. */
    PATH_CACHE_MODULES(".bach/modules"),

    /** Name of the project. */
    PROJECT_NAME("project"),

    /** Version of the project. */
    PROJECT_VERSION("1.0.0-SNAPSHOT"),

    /** URI to Google Java Format "all-deps" JAR. */
    TOOL_FORMAT_URI(
        "https://github.com/"
            + "google/google-java-format/releases/download/google-java-format-1.7/"
            + "google-java-format-1.7-all-deps.jar"),

    /** URI to JUnit Platform Console Standalone JAR. */
    TOOL_JUNIT_URI(
        "http://central.maven.org/"
            + "maven2/org/junit/platform/junit-platform-console-standalone/1.4.0/"
            + "junit-platform-console-standalone-1.4.0.jar"),

    /** Maven URI. */
    TOOL_MAVEN_URI(
        "https://archive.apache.org/"
            + "dist/maven/maven-3/3.6.0/binaries/"
            + "apache-maven-3.6.0-bin.zip");

    /** Load from properties from path. */
    static Properties loadProperties(Path path) {
      var properties = new Properties();
      if (Files.exists(path)) {
        try (var stream = Files.newInputStream(path)) {
          properties.load(stream);
        } catch (Exception e) {
          throw new Error("Loading properties failed: " + path, e);
        }
      }
      return properties;
    }

    final String key;
    final String defaultValue;

    Property(String defaultValue) {
      this.key = "bach." + name().toLowerCase().replace('_', '.');
      this.defaultValue = defaultValue;
    }
  }

  /** Bach consumer/visitor. */
  interface Task {

    static Task of(String tool, Iterable<?> arguments) {
      switch (tool) {
        case "format":
        case "fmt":
          return new Tool.Format(arguments);
        case "junit":
          return new Tool.JUnit(arguments);
        case "maven":
        case "mvn":
          return new Tool.Maven(arguments);
        default:
          return new Command(tool).addAll(arguments);
      }
    }

    void execute(Bach bach);

    default String name() {
      return toString();
    }
  }

  /** External program. */
  interface Tool extends Task {

    Command toCommand(Bach bach) throws Exception;

    static Path tools(Bach bach) {
      return USER_HOME.resolve(bach.get(Property.PATH_CACHE_TOOLS));
    }

    @Override
    default void execute(Bach bach) {
      try {
        toCommand(bach).execute(bach);
      } catch (Exception e) {
        throw new Error("Execution of " + this + " failed!", e);
      }
    }

    class Format implements Tool {

      static Path install(Bach bach) throws Exception {
        var name = "google-java-format";
        var destination = Files.createDirectories(tools(bach).resolve(name));
        var uri = URI.create(bach.get(Property.TOOL_FORMAT_URI));
        return bach.download(destination, uri);
      }

      final Iterable<?> arguments;
      final Collection<Path> roots;

      Format(Iterable<?> arguments) {
        this.arguments = arguments;
        this.roots = List.of();
      }

      Format(boolean replace, Collection<Path> roots) {
        this.arguments =
            replace ? List.of("--replace") : List.of("--dry-run", "--set-exit-if-changed");
        this.roots = roots;
      }

      @Override
      public Command toCommand(Bach bach) throws Exception {
        var jar = install(bach);
        var command = new Command("java");
        command.add("-jar");
        command.add(jar);
        command.addAll(arguments);
        // command.mark(10);
        command.addAllJavaFiles(roots);
        return command;
      }
    }

    class JUnit implements Tool {

      static Path install(Bach bach) throws Exception {
        var name = "junit-platform-console-standalone";
        var destination = Files.createDirectories(tools(bach).resolve(name));
        var uri = URI.create(bach.get(Property.TOOL_JUNIT_URI));
        return bach.download(destination, uri);
      }

      final Iterable<?> arguments;

      JUnit(Iterable<?> arguments) {
        this.arguments = arguments;
      }

      @Override
      public Command toCommand(Bach bach) throws Exception {
        var junit = install(bach);
        var command = new Command("java");
        command.add("-ea");
        command.add("-jar").add(junit);
        command.addAll(arguments);
        return command;
      }
    }

    class Maven implements Tool {

      final Iterable<?> arguments;

      Maven(Iterable<?> arguments) {
        this.arguments = arguments;
      }

      @Override
      public Command toCommand(Bach bach) throws Exception {
        var uri = URI.create(bach.get(Property.TOOL_MAVEN_URI));
        var zip = bach.download(Files.createDirectories(tools(bach)), uri);
        var home = bach.extract(zip);
        var win = System.getProperty("os.name").toLowerCase().contains("win");
        var name = "mvn" + (win ? ".cmd" : "");
        var executable = home.resolve("bin").resolve(name);
        executable.toFile().setExecutable(true);
        return new Command(executable.toString()).addAll(arguments);
      }
    }
  }
}
