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

  private DynamicTest newDynamicTest(String name, Path temp) {
    var uri = DEMO.resolve(name).resolve("src/a/main/java/module-info.java").toUri();
    return DynamicTest.dynamicTest(name, uri, () -> demo(name, temp.resolve(name)));
  }

  private void demo(String name, Path work) throws Exception {
    var demo = Demo.build(name, Files.createDirectories(work));
    assertLinesMatch(List.of(), demo.errors());
    assertLinesMatch(List.of(">> BUILD >>", "Bach::build() end."), demo.lines());
  }

  static class Demo implements AutoCloseable {

    static Demo build(String name, Path work) {
      try (var demo = new Demo(DEMO.resolve(name), work)) {
        demo.bach.build();
        return demo;
      }
    }

    final StringWriter out, err;
    final Bach bach;

    Demo(Path home, Path work) {
      this.out = new StringWriter();
      this.err = new StringWriter();
      this.bach = new Bach(new PrintWriter(out), new PrintWriter(err), home, work, true);
    }

    @Override
    public void close() {
      out.flush();
      err.flush();
    }

    List<String> lines() {
      return out.toString().lines().collect(Collectors.toList());
    }

    List<String> errors() {
      return err.toString().lines().collect(Collectors.toList());
    }
  }
}
