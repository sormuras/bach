import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

/** Step 4 - Composing tool finders. */
class Step4 {
  public static void main(String... args) {
    /* Empty args array given? Show usage message and exit. */ {
      if (args.length == 0) {
        System.out.println("Usage: Step4 TOOL-NAME [TOOL-ARGS...]");
        return;
      }
    }

    var finder =
        ToolFinder.compose(
            //
            ToolFinder.of(new Banner()),
            //
            ToolFinder.ofSystem()
            //
            );

    /* Handle special case: --list-tools */ {
      if (args[0].equals("--list-tools")) {
        finder.findAll().forEach(tool -> System.out.printf("%9s by %s%n", tool.name(), tool));
        return;
      }
    }

    var runner = ToolRunner.of(finder);
    runner.run(args[0], Arrays.copyOfRange(args, 1, args.length));
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
      record ListToolFinder(List<ToolProvider> findAll) implements ToolFinder {}
      return new ListToolFinder(List.of(providers));
    }

    static ToolFinder ofSystem() {
      return () ->
          ServiceLoader.load(ToolProvider.class, ClassLoader.getSystemClassLoader()).stream()
              .map(ServiceLoader.Provider::get)
              .toList();
    }
  }

  interface ToolRunner {

    ToolFinder finder();

    void run(String name, String... args);

    static ToolRunner of(ToolFinder finder) {
      record DefaultToolRunner(ToolFinder finder) implements ToolRunner {
        public void run(String name, String... args) {
          var code = finder().find(name).orElseThrow().run(System.out, System.err, args);
          if (code != 0) throw new RuntimeException(name + " returned non-zero code: " + code);
        }
      }
      return new DefaultToolRunner(finder);
    }
  }

  record Banner(String name) implements ToolProvider {

    Banner() {
      this("banner");
    }

    @Override
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
}

// Next step:
// [ ] How to implement a tool that runs other tools?
// [ ] Add an abstraction for tool running tool: an operator
