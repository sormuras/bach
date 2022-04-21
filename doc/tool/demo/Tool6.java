import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

/** Compile and link modular example application. */
class Tool6 {
  public static void main(String... args) {
    /* Empty args array given? Show usage message and exit. */ {
      if (args.length == 0) {
        System.err.printf("Usage: %s TOOL-NAME TOOL-ARGS...%n", Tool6.class.getSimpleName());
        return;
      }
    }

    var finder =
        ToolFinder.compose(
            ToolFinder.of(new Banner(), new Chain()),
            ToolFinder.of(new Compile(), new Link()),
            ToolFinder.ofSystem());

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
              .forEach(tool -> System.out.printf("%9s by %s%n", tool.name(), tool));
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

  interface ToolFinder {
    List<ToolProvider> findAll();

    default Optional<ToolProvider> find(String name) {
      return streamAll().filter(tool -> tool.name().equals(name)).findFirst();
    }

    default Stream<ToolProvider> streamAll() {
      return findAll().stream();
    }

    static ToolFinder compose(ToolFinder... finders) {
      record CompositeToolFinder(List<ToolFinder> finders) implements ToolFinder {
        public List<ToolProvider> findAll() {
          return finders.stream().flatMap(ToolFinder::streamAll).toList();
        }
      }
      return new CompositeToolFinder(List.of(finders));
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
  }

  interface ToolRunner {
    void run(String name, String... args);

    static ToolRunner of(ToolFinder finder) {
      return new ToolRunner() {
        public void run(String name, String... args) {
          var tool = finder.find(name).orElseThrow();
          var code =
              tool instanceof ToolOperator operator
                  ? operator.run(this, args)
                  : tool.run(System.out, System.err, args);
          if (code != 0) throw new RuntimeException(name + " returned non-zero code: " + code);
        }
      };
    }
  }

  interface ToolOperator extends ToolProvider {
    default int run(PrintWriter out, PrintWriter err, String... args) {
      return run(ToolRunner.of(ToolFinder.ofSystem()), args);
    }

    int run(ToolRunner runner, String... args);
  }

  record Banner(String name) implements ToolProvider {
    Banner() {
      this("banner");
    }

    public int run(PrintWriter out, PrintWriter err, String... args) {
      var text = "USAGE: %s TEXT...".formatted(name());
      var line = args.length != 0 ? String.join(" ", args) : text;
      var dash = "=".repeat(line.length());
      out.printf(
          """
          %s
          %s
          %s
          """,
          dash, line.toUpperCase(), dash);
      return 0;
    }
  }

  record Chain(String name) implements ToolOperator {
    Chain() {
      this("chain");
    }

    public int run(ToolRunner runner, String... args) {
      for (var name : args) runner.run(name); // no tool args
      return 0;
    }
  }

  record Compile(String name) implements ToolOperator {
    Compile() {
      this("compile");
    }

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
                  "--verbose",
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

    public int run(ToolRunner runner, String... args) {
      runner.run(
          "jlink",
          "--verbose",
          "--output=.bach/out/image",
          "--module-path=.bach/out",
          "--add-modules=org.example",
          "--launcher=example=org.example.app/org.example.app.Main");
      return 0;
    }
  }
}
