import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import java.util.stream.StreamSupport;

/** JDK Tools and Where to Find and How to Run Them. */
class Tool {
  static final Path JAVA_HOME = Path.of(System.getProperty("java.home", "."));

  public static void main(String... args) {
    /* Empty args array given? Show usage message and exit. */ {
      if (args.length == 0) {
        System.err.printf("Usage: %s TOOL-NAME TOOL-ARGS...%n", Tool.class.getSimpleName());
        return;
      }
    }

    var finder =
        ToolFinder.compose(
            ToolFinder.of(new Banner(), new Chain()),
            ToolFinder.of(new Compile(), new Link(), new Run()),
            ToolFinder.ofSystem(),
            ToolFinder.ofNativeTools(JAVA_HOME.resolve("bin")));

    /* Handle special case: --list-tools */ {
      if (args[0].equals("--list-tools")) {
        var tools = finder.findAll();
        /* An empty tool finder? */ if (tools.isEmpty()) {
          System.out.println("No tool found. Using an empty tool finder?");
          return;
        }
        /* List all tools sorted by name. */ {
          tools.stream()
              .sorted(Comparator.comparing(ToolProvider::name))
              .forEach(tool -> System.out.printf("%16s by %s%n", tool.name(), tool));
          System.out.printf("%n  %d tool%s%n", tools.size(), tools.size() == 1 ? "" : "s");
        }
        return;
      }
    }

    /* Run an arbitrary tool. */ {
      var runner = ToolRunner.of(finder);
      runner.run(args[0], Arrays.copyOfRange(args, 1, args.length));
    }
  }

  interface ToolRunner {
    void run(String name, String... args);

    static ToolRunner of(ToolFinder finder) {
      return new ToolRunner() {
        public void run(String name, String... args) {
          System.out.printf("| %s %s%n", name, String.join(" ", args));
          var tool = finder.find(name).orElseThrow(() -> new NoSuchElementException(name));
          var code =
              tool instanceof ToolOperator operator
                  ? operator.run(this, args)
                  : tool.run(System.out, System.err, args);
          if (code != 0) throw new RuntimeException(name + " returned non-zero code: " + code);
        }
      };
    }
  }

  interface ToolFinder {

    List<? extends ToolProvider> findAll();

    default Optional<? extends ToolProvider> find(String name) {
      return findAll().stream().filter(tool -> tool.name().equals(name)).findFirst();
    }

    static ToolFinder compose(ToolFinder... finders) {
      record CompositeFinder(List<ToolFinder> finders) implements ToolFinder {
        public List<? extends ToolProvider> findAll() {
          return finders.stream().flatMap(finder -> finder.findAll().stream()).toList();
        }

        public Optional<? extends ToolProvider> find(String name) {
          for (var finder : finders) {
            var tool = finder.find(name);
            if (tool.isPresent()) return tool;
          }
          return Optional.empty();
        }
      }
      return new CompositeFinder(List.of(finders));
    }

    static ToolFinder of(ToolProvider... providers) {
      record DirectToolFinder(List<ToolProvider> findAll) implements ToolFinder {}
      return new DirectToolFinder(List.of(providers));
    }

    static ToolFinder ofSystem() {
      return () ->
          ServiceLoader.load(ToolProvider.class, ClassLoader.getSystemClassLoader()).stream()
              .map(ServiceLoader.Provider::get)
              .toList();
    }

    static ToolFinder ofNativeTools(Path directory) {
      record NativePrograms(Path path) implements ToolFinder {
        public List<? extends ToolProvider> findAll() {
          try (var files = Files.newDirectoryStream(path, Files::isRegularFile)) {
            return StreamSupport.stream(files.spliterator(), false)
                .filter(NativeToolProvider::isExecutable)
                .map(NativeToolProvider::of)
                .toList();

          } catch (Exception exception) {
            throw new RuntimeException(exception);
          }
        }
      }
      return new NativePrograms(directory);
    }
  }

  interface ToolOperator extends ToolProvider {
    default String name() {
      return getClass().getSimpleName();
    }

    default int run(PrintWriter out, PrintWriter err, String... args) {
      throw new UnsupportedOperationException();
    }

    int run(ToolRunner runner, String... args);
  }

  record Banner(String name) implements ToolProvider {
    Banner() {
      this("banner");
    }

    public int run(PrintWriter out, PrintWriter err, String... args) {
      var text = args.length == 0 ? "Usage: banner TEXT..." : String.join(" ", args).toUpperCase();
      var dash = "=".repeat(text.length());
      out.printf("""
          %s
          %s
          %s
          """, dash, text, dash);
      return 0;
    }
  }

  record Chain(String name) implements ToolOperator {
    Chain() {
      this("chain");
    }

    public int run(ToolRunner runner, String... args) {
      for (var name : args) runner.run(name);
      return 0;
    }
  }

  record Compile(String name) implements ToolOperator {

    Compile() {
      this("compile");
    }

    @Override
    public int run(ToolRunner runner, String... args) {
      var modules = List.of("org.example", "org.example.app", "org.example.lib");
      var out = Path.of(".bach", "out");
      runner.run(
          "javac",
          "-d",
          ".bach/out/classes",
          "--module-source-path=.",
          "--module=" + String.join(",", modules));
      modules.forEach(
          module ->
              runner.run(
                  "jar",
                  "--create",
                  "--file=" + out.resolve(module + ".jar"),
                  "-C",
                  "" + out.resolve("classes/" + module),
                  "."));
      return 0;
    }
  }

  record Link(String name) implements ToolOperator {
    Link() {
      this("link");
    }

    @Override
    public int run(ToolRunner runner, String... args) {
      runner.run(
          "jlink",
          "--output=.bach/out/image",
          "--module-path=.bach/out",
          "--add-modules=org.example",
          "--launcher=example=org.example.app/org.example.app.Main");
      return 0;
    }
  }

  record NativeToolProvider(String name, List<String> command) implements ToolProvider {
    static final boolean WINDOWS = System.getProperty("os.name", "?").toLowerCase().contains("win");

    static boolean isExecutable(Path path) {
      if (path.getNameCount() == 0) return false;
      if (!Files.isExecutable(path)) return false;
      return !WINDOWS || path.getFileName().toString().endsWith(".exe");
    }

    static NativeToolProvider of(Path executable) {
      var file = executable.getFileName().toString();
      var name = file.endsWith(".exe") ? file.substring(0, file.length() - 4) : file;
      var path = executable.toAbsolutePath().normalize().toString();
      return new NativeToolProvider(name, List.of(path));
    }

    public int run(PrintWriter out, PrintWriter err, String... arguments) {
      record LinePrinter(InputStream stream, PrintWriter writer) implements Runnable {
        public void run() {
          new BufferedReader(new InputStreamReader(stream)).lines().forEach(writer::println);
        }
      }
      var builder = new ProcessBuilder(new ArrayList<>(command));
      builder.command().addAll(List.of(arguments));
      try {
        var process = builder.start();
        new Thread(new LinePrinter(process.getInputStream(), out)).start();
        new Thread(new LinePrinter(process.getErrorStream(), err)).start();
        return process.waitFor();
      } catch (Exception exception) {
        exception.printStackTrace(err);
        return -1;
      }
    }
  }

  record Run(String name) implements ToolOperator {
    Run() {
      this("run");
    }

    @Override
    public int run(ToolRunner runner, String... args) {
      try {
        var java = new ArrayList<String>();
        java.add(".bach/out/image/bin/java");
        if (args.length == 0) java.add("--module=org.example.app/org.example.app.Main");
        java.addAll(List.of(args));
        return new ProcessBuilder(java).inheritIO().start().waitFor();
      } catch (Exception e) {
        e.printStackTrace(System.err);
        return -1;
      }
    }
  }
}
