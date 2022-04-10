import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;

/** Introduce ToolFinder to list observable tools. */
class Tool2 {
  public static void main(String... args) {
    /* Empty args array given? Show usage message and exit. */ {
      if (args.length == 0) {
        System.err.printf("Usage: %s TOOL-NAME TOOL-ARGS...%n", Tool2.class.getSimpleName());
        return;
      }
    }

    ToolFinder finder = List::of;

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
      return findAll().stream().filter(tool -> tool.name().equals(name)).findFirst();
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
      return (name, args) -> {
        var tool = finder.find(name).orElseThrow(() -> new RuntimeException(name + " not found"));
        var code = tool.run(System.out, System.err, args);
        if (code != 0) throw new RuntimeException(name + " returned non-zero code: " + code);
      };
    }
  }
}
