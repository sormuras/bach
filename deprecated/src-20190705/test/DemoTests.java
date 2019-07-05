import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;

class DemoTests {

  private static final Path DEMO = Path.of("src", "demo");

  @TestFactory
  Stream<DynamicTest> demo(@TempDir Path temp) {
    return Bach.Util.findDirectoryNames(DEMO).stream().map(name -> newDynamicTest(name, temp));
  }

  private static DynamicTest newDynamicTest(String name, Path temp) {
    var uri = DEMO.resolve(name).resolve("src/a/main/java/module-info.java").toUri();
    return DynamicTest.dynamicTest(name, uri, () -> demo(name, temp.resolve(name)));
  }

  private static void demo(String name, Path work) throws Exception {
    var demo = new Demo(DEMO.resolve(name), Files.createDirectories(work));
    try (demo) {
      demo.bach.build();
      assertLinesMatch(List.of(), demo.errors());
      assertLinesMatch(List.of(">> BUILD >>", "Bach::build() end."), demo.lines());
    } catch (Throwable throwable) {
      if (expected(name, work, throwable)) {
        return;
      }
      System.out.println(demo.outputWriter);
      Files.readAllLines(work.resolve("process-out.txt")).forEach(System.out::println);
      System.err.println(demo.errorWriter);
      Files.readAllLines(work.resolve("process-err.txt")).forEach(System.err::println);
      throw throwable;
    }
  }

  private static boolean expected(String name, Path work, Throwable throwable) throws Exception {
    switch (name) {
      case "001-test-t":
        assertEquals("No tests found!", throwable.getMessage());
        assertLinesMatch(
            List.of(">> TEST TREE >>", "[         0 tests found           ]", ">> SUMMARY >>"),
            Files.readAllLines(work.resolve("process-out.txt")));
        return true;
      case "100-main-a-test-a,t":
        assertEquals("Test run failed!", throwable.getMessage());
        return true;
    }
    return false;
  }

  static class Demo implements AutoCloseable {

    final StringWriter outputWriter, errorWriter; // for internal/in-process messages
    final Path outputPath, errorPath; // for external processes
    final Bach bach;

    Demo(Path home, Path work) {
      this.outputWriter = new StringWriter();
      this.errorWriter = new StringWriter();
      this.outputPath = work.resolve("process-out.txt");
      this.errorPath = work.resolve("process-err.txt");
      this.bach =
          new Bach(
              new PrintWriter(outputWriter),
              new PrintWriter(errorWriter),
              it -> it.redirectOutput(outputPath.toFile()).redirectError(errorPath.toFile()),
              home,
              work,
              true);
    }

    @Override
    public void close() {
      outputWriter.flush();
      errorWriter.flush();
    }

    List<String> lines() {
      return outputWriter.toString().lines().collect(Collectors.toList());
    }

    List<String> errors() {
      return errorWriter.toString().lines().collect(Collectors.toList());
    }
  }
}
