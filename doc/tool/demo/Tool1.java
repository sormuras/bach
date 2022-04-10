import java.util.Arrays;
import java.util.spi.ToolProvider;

/** Step 1 - An application running an arbitrary tool. */
class Tool1 {
  public static void main(String... args) {
    /* Empty args array given? Show usage message and exit. */ {
      if (args.length == 0) {
        System.out.println("Usage: Step1 TOOL-NAME [TOOL-ARGS...]");
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
        var code = ToolProvider.findFirst(name).orElseThrow().run(System.out, System.err, args);
        if (code != 0) throw new RuntimeException(name + " returned non-zero code: " + code);
      };
    }
  }
}
