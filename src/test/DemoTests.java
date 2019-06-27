import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

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
  Stream<DynamicTest> demo(@TempDir Path work) {
    return Bach.Util.findDirectoryNames(DEMO).stream()
        .map(name -> dynamicTest(name, () -> demo(name, work.resolve(name))));
  }

  private void demo(String name, Path work) throws Exception {
    var demo = Demo.build(name, Files.createDirectories(work));
    assertLinesMatch(List.of(), demo.errors());
    assertLinesMatch(List.of(">> BUILD >>", "Bach::build() end."), demo.lines());
  }

  static class Demo {

    static Demo build(String name, Path work) {
      var demo = new Demo(DEMO.resolve(name), work);
      demo.bach.build();
      return demo;
    }

    final StringWriter out, err;
    final Bach bach;

    Demo(Path home, Path work) {
      this.out = new StringWriter();
      this.err = new StringWriter();
      this.bach = new Bach(new PrintWriter(out), new PrintWriter(err), home, work, true);
    }

    List<String> lines() {
      return out.toString().lines().collect(Collectors.toList());
    }

    List<String> errors() {
      return err.toString().lines().collect(Collectors.toList());
    }
  }
}
