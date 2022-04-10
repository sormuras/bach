import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.spi.ToolProvider;

/** JDK Tools and Where to Find and How to Run Them. */
class Tool {
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
            ToolFinder.ofSystem());

    /* Handle special case: --list-tools */ {
      if (args[0].equals("--list-tools")) {
        finder.forEach(tool -> System.out.printf("%9s = %s%n", tool.name(), tool));
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

    List<ToolProvider> findAll();

    default Optional<ToolProvider> find(String name) {
      return findAll().stream().filter(tool -> tool.name().equals(name)).findFirst();
    }

    default void forEach(Consumer<ToolProvider> consumer) {
      findAll().stream().sorted(Comparator.comparing(ToolProvider::name)).forEach(consumer);
    }

    static ToolFinder compose(ToolFinder... finders) {
      record CompositeFinder(List<ToolFinder> finders) implements ToolFinder {
        public List<ToolProvider> findAll() {
          return finders.stream().flatMap(finder -> finder.findAll().stream()).toList();
        }

        public Optional<ToolProvider> find(String name) {
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
