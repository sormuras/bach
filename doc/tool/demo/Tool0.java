import java.util.spi.ToolProvider;

/** Step 0 - Use {@code ToolProvider} SPI to find and run {@code javac} tool. */
class Tool0 {
  public static void main(String... args) {
    ToolProvider.findFirst("javac").orElseThrow().run(System.out, System.err, args);
  }
}
