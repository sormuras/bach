import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;

/**
 * Step 2 - Introduce ToolFinder to list observable tools.
 *
 * <li>Create ToolFinder interface with abstract List<ToolProvider> findAll() method
 * <li>Add default Optional<ToolProvider> find(NAME) method to ToolFinder
 * <li>Add ToolFinder.ofEmpty() factory
 * <li>In ToolRunner, replace ToolProvider.findFirst(NAME) usage with ToolFinder.find(NAME)
 * <li>TODO Implement ToolFinder.ofSystem() by looking into ToolProvider.findFirst(NAME)
 */
class Step2 {
  public static void main(String... args) {
    /* Empty args array given? Show usage message and exit. */ {
      if (args.length == 0) {
        System.out.println("Usage: Step2 TOOL-NAME [TOOL-ARGS...]");
        return;
      }
    }

    var finder = ToolFinder.ofEmpty();

    /* Handle special case: --list-tools */ {
      if (args[0].equals("--list-tools")) {
        finder.findAll().stream()
            .sorted(Comparator.comparing(ToolProvider::name))
            .forEach(tool -> System.out.printf("%9s by %s%n", tool.name(), tool));
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

    static ToolFinder ofEmpty() {
      return List::of;
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
        @Override
        public void run(String name, String... args) {
          var code = finder().find(name).orElseThrow().run(System.out, System.err, args);
          if (code != 0) throw new RuntimeException(name + " returned non-zero code: " + code);
        }
      }
      return new DefaultToolRunner(finder);
    }
  }
}

// HINT:
// [x] Run: java --limit-modules java.base demo/Step2.java

// NEXT:
//  ?  Run: java demo/Step2.java banner hello world
// [ ] Implement a custom tool: `record Banner(String name) implements ToolProvider {...}`
// [ ] Implement a tool finder that accepts instances of `ToolProvider`
