import java.util.Arrays;
import java.util.spi.ToolProvider;

/** An application running an arbitrary tool. */
class Tool1 {
  public static void main(String... args) {
    /* Empty args array given? Show usage message and exit. */ {
      if (args.length == 0) {
        System.err.printf("Usage: %s TOOL-NAME TOOL-ARGS...%n", Tool1.class.getSimpleName());
        return;
      }
    }

    /* Run an arbitrary tool. */ {
      var runner = ToolRunner.of();
      runner.run(args[0], Arrays.copyOfRange(args, 1, args.length));
    }
  }

  interface ToolRunner {
    void run(String name, String... args);

    static ToolRunner of() {
      return (name, args) -> {
        var tool = ToolProvider.findFirst(name).orElseThrow();
        var code = tool.run(System.out, System.err, args);
        if (code != 0) throw new RuntimeException(name + " returned non-zero code: " + code);
      };
    }
  }
}
