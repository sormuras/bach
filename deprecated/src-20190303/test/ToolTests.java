import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@BachExtension
class ToolTests {

  static List<String> start(String tool, String... args) throws Exception {
    return start(23, 0, tool, args);
  }

  static List<String> start(int timeout, int expected, String tool, String... args)
      throws Exception {
    var temp = Files.createTempDirectory("start-" + tool + "-");
    var text = temp.resolve("out.txt");
    var builder = new ProcessBuilder("java");
    builder.command().add("-ea");
    builder.command().add("-Debug=true");
    builder.command().add("src/bach/Bach.java");
    builder.command().add("tool");
    builder.command().add(tool);
    Arrays.stream(args).forEach(builder.command()::add);
    try {
      var process = builder.redirectErrorStream(true).redirectOutput(text.toFile()).start();
      if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
        process.destroy();
      }
      var value = process.exitValue();
      var lines = Files.readAllLines(text);
      assertEquals(
          expected,
          process.exitValue(),
          "Expected exit value of "
              + expected
              + ", but got: "
              + value
              + "\n"
              + String.join("\n", lines));
      return lines;
    } finally {
      Files.deleteIfExists(text);
      Files.deleteIfExists(temp);
    }
  }

  @Nested
  class JUnit {
    @Test
    void help() throws Exception {
      assertLinesMatch(
          List.of(
              "DEBUG Running 1 task(s)...",
              ">> INSTALL AND INVOKE JUNIT >>",
              "Usage: ConsoleLauncher [-h] [--disable-ansi-colors] [--disable-banner]",
              ">> MORE OPTIONS >>"),
          start("junit", "--help"));
    }

    @Test
    void version() throws Exception {
      assertLinesMatch(
          List.of(
              "DEBUG Running 1 task(s)...",
              ">> INSTALL AND INVOKE JUNIT >>",
              "Error parsing command-line arguments: Unknown option: --version",
              ">> HELP >>"),
          start(23, 1, "junit", "--version"));
    }
  }

  @Nested
  class GoogleJavaFormat {
    @Test
    void help() throws Exception {
      assertLinesMatch(
          List.of(
              "DEBUG Running 1 task(s)...",
              "\\QDEBUG >> Bach$Tool$Format\\E.+",
              ">> INSTALL AND INVOKE GOOGLE-JAVA-FORMAT >>",
              "Usage: google-java-format [options] file(s)",
              ">> HELP TEXT >>",
              "\\QDEBUG << Bach$Tool$Format\\E.+"),
          start("format", "--help"));
    }
  }

  @Nested
  class Maven {

    @Test
    void version() throws Exception {
      assertLinesMatch(
          List.of(
              "DEBUG Running 1 task(s)...",
              "\\QDEBUG >> Bach$Tool$Maven\\E.+",
              ">> INSTALL AND INVOKE MAVEN >>",
              "\\QApache Maven 3.6.0 (97c98ec64a1fdfee7767ce5ffb20918da4f719f3; 2018-10-24T\\E.+",
              ">> MORE VERSIONS >>",
              "\\QDEBUG << Bach$Tool$Maven\\E.+"),
          start("maven", "--version"));
    }
  }

  static class NoopTool implements Bach.Tool {

    @Override
    public String name() {
      return "Noop";
    }

    @Override
    public Bach.Command toCommand(Bach bach) {
      throw new UnsupportedOperationException("Noop");
    }

    @Override
    public String toString() {
      return name();
    }
  }

  @Test
  void runNoopToolWithNonZeroCodeFails(BachExtension.BachSupplier supplier) {
    var bach = supplier.get();
    var noop = new NoopTool();
    assertEquals(1, bach.run(List.of(noop)));
    var e = assertThrows(Error.class, () -> noop.execute(bach));
    assertTrue(UnsupportedOperationException.class.isAssignableFrom(e.getCause().getClass()));
    assertLinesMatch(
        List.of(
            "java.lang.Error: Execution of Noop failed!",
            ">> STACKTRACE >>",
            "Caused by: java.lang.UnsupportedOperationException: Noop",
            ">> STACKTRACE >>"),
        supplier.errLines());
  }
}
