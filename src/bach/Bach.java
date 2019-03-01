import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.WARNING;

import java.io.File;
import java.io.PrintStream;
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
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Java Shell Builder. */
class Bach {

  static final String VERSION = "master";

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
            throw new Error("No name supplied for TOOL action!");
          }
          var name = arguments.removeFirst();
          var task = new Command(name).addAll(arguments);
          tasks.add(task);
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
    log(INFO, String.format("downloading %s to %s <- %s", name, destination, uri));
    try (var stream = uri.toURL().openStream()) {
      Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
    }
    return target;
  }

  /** Format all Java source units. */
  void format() {
    // TODO Format with "--replace".
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

    /** Dump command executables and arguments using the provided string consumer. */
    Command dump(Consumer<String> consumer) {
      var iterator = arguments.listIterator();
      consumer.accept(name);
      while (iterator.hasNext()) {
        var argument = iterator.next();
        var indent = argument.startsWith("-") ? "" : "  ";
        consumer.accept(indent + argument);
      }
      return this;
    }

    @Override
    public void execute(Bach bach) {
      bach.run(name, arguments.toArray(String[]::new));
    }
  }

  /** Bach consumer/visitor. */
  interface Task {

    void execute(Bach bach);

    default String name() {
      return toString();
    }
  }
}
