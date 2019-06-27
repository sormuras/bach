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
    var demo = Demo.build("000-main(a)", work);
    assertLinesMatch(List.of(), demo.errors());
    assertLinesMatch(
        List.of(">> INFO >>", "  modules = [a]", ">> BUILD >>", "Bach::build() end."),
        demo.lines());
  }

  @Test
  void demo001(@TempDir Path work) {
    var demo = Demo.build("001-test(t)", work);
    assertLinesMatch(List.of(), demo.errors());
    assertLinesMatch(
        List.of(">> INFO >>", "  modules = [t]", ">> BUILD >>", "Bach::build() end."),
        demo.lines());
  }

  @Test
  void demo010(@TempDir Path work) {
    var demo = Demo.build("010-main(a,b,c)", work);
    assertLinesMatch(List.of(), demo.errors());
    assertLinesMatch(
        List.of(">> INFO >>", "  modules = [a, b, c]", ">> BUILD >>", "Bach::build() end."),
        demo.lines());
  }

  static class Demo {

    static Demo build(String name, Path work) {
      var demo = new Demo(Path.of("src", "demo", name), work);
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
