import java.util.Arrays;
import java.util.spi.ToolProvider;

/**
 * Step 1 - An application running an arbitrary tool.
 *
 * <li>Show usage message on empty args array
 * <li>Create {@code ToolRunner} interface with run method
 * <li>Move find and run code into default implementation
 * <li>TODO Print tool name and its args on run
 */
class Step1 {
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

// DONE
// [x] Let args[0] be the name of the tool to run and args[1..n] its arguments
// [x] Added an abstraction for running a tool, throwing on non-zero exit code

// HINT
// [ ] System.out.println("// name = " + name + ", args = " + Arrays.deepToString(args));
// [ ] Run with well-known system tools: javac, jar, jlink

// NEXT
//  ?  Run with: jfr
// --> Implement a `--list-tools` option showing all observable tools and exit
// --> By introducing a configurable `ToolFinder` abstraction
