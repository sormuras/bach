import java.util.spi.ToolProvider;

/** Use tool provider to find and run javac tool. */
class Tool0 {
  public static void main(String... args) {
    ToolProvider.findFirst("javac").orElseThrow().run(System.out, System.err, "--version");
  }
}
