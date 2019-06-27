import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DemoTests {

  @Test
  void demo000(@TempDir Path work) {
    var demo = Demo.build(Path.of("demo", "000-main(a)"), work);
    assertLinesMatch(List.of(), demo.errors());
    assertLinesMatch(
        List.of("Bach .+ initialized.", ">> BUILD >>", "Bach::build() end."), demo.lines());
  }

  static class Demo {

    static Demo build(Path home, Path work) {
      var demo = new Demo(home, work);
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
