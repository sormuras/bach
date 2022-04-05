import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;

record Tool(String name, ToolProvider descriptor) {

  public static void main(String... args) {
    var finder =
        Finder.compose(
            Finder.of(Tool.of(new Banner()), Tool.of(new Chain())),
            // Finder.of(Tool.of("jar"), Tool.of("javac"), Tool.of("javadoc"), Tool.of("jlink")),
            Finder.ofSystemTools());

    finder.findAll().stream().sorted(Comparator.comparing(Tool::name)).forEach(System.out::println);

    if (args.length == 0) {
      finder.run("banner", "a", "b", "c");
      finder.run("javac", "--version");
      return;
    }

    finder.run(args[0], Arrays.copyOfRange(args, 1, args.length));
  }

  static Tool of(String name) {
    return Tool.of(ToolProvider.findFirst(name).orElseThrow());
  }

  static Tool of(ToolProvider provider) {
    return new Tool(provider.name(), provider);
  }

  interface Runner {
    int run(String name, String... args);
  }

  interface Finder extends Runner {

    List<Tool> findAll();

    default Optional<Tool> find(String name) {
      return findAll().stream().filter(tool -> tool.name().equals(name)).findFirst();
    }

    default int run(String name, String... args) {
      var provider = find(name).orElseThrow(() -> new NoSuchElementException(name)).descriptor();
      return provider instanceof Operator operator
          ? operator.run(this, args)
          : provider.run(OUT, ERR, args);
    }

    static Finder compose(Finder... finders) {
      record CompositeFinder(List<Finder> finders) implements Finder {
        public List<Tool> findAll() {
          return finders.stream().flatMap(finder -> finder.findAll().stream()).toList();
        }

        public Optional<Tool> find(String name) {
          for (var finder : finders) {
            var tool = finder.find(name);
            if (tool.isPresent()) return tool;
          }
          return Optional.empty();
        }
      }
      return new CompositeFinder(List.of(finders));
    }

    static Finder of(Tool... tools) {
      record DirectToolFinder(List<Tool> findAll) implements Finder {}
      return new DirectToolFinder(List.of(tools));
    }

    static Finder of(ClassLoader loader) {
      return Finder.of(ServiceLoader.load(ToolProvider.class, loader));
    }

    static Finder of(ServiceLoader<ToolProvider> loader) {
      record ServiceLoaderFinder(ServiceLoader<ToolProvider> loader) implements Finder {
        public List<Tool> findAll() {
          synchronized (loader) {
            return loader.stream().map(ServiceLoader.Provider::get).map(Tool::of).toList();
          }
        }
      }
      return new ServiceLoaderFinder(loader);
    }

    static Finder ofSystemTools() {
      return Finder.of(ClassLoader.getSystemClassLoader());
    }

    PrintWriter OUT = new PrintWriter(System.out, true);
    PrintWriter ERR = new PrintWriter(System.err, true);
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

  interface Operator extends ToolProvider {
    default int run(PrintWriter out, PrintWriter err, String... args) {
      throw new UnsupportedOperationException();
    }

    int run(Runner runner, String... args);
  }

  record Chain(String name) implements Operator {
    Chain() {
      this("chain");
    }

    public int run(Runner runner, String... args) {
      for (var name : args) {
        var code = runner.run(name);
        if (code != 0) return code;
      }
      return 0;
    }
  }
}
