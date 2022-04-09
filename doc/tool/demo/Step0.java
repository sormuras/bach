import java.util.spi.ToolProvider;

/**
 * Step 0 - Use {@code ToolProvider} SPI to find and run {@code javac} tool.
 * <li>Launch single-file source-code Java program (JEP 330)
 * <li>Run: {@code java demo/Step0.java}
 * <li><b>TODO</b> <i>Pass non-empty main args as tool's args</i>
 * <li>Run: {@code java demo/Step0.java --version}
 */
class Step0 {
  public static void main(String... args) {
    ToolProvider
        .findFirst("javac").orElseThrow()
        .run(System.out, System.err, args.length != 0 ? args : new String[] {"--version"});
  }
}

// === DONE ===
// [x] Used ToolProvider SPI
// [x] Ignored result of the tool execution

// === HINT ===
// [ ] args.length != 0 ? args : new String[] {"--version"}
// [ ] Run: java demo/Step0.java --help-extra
// [ ] Run: java demo/Step0.java --help-lint

// === NEXT ===
//  ?  Run: java demo/Step0.java jar --version
// --> Transform into an application running an arbitrary tool
