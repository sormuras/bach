package tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.ToolResponse;
import com.github.sormuras.bach.ToolRunner;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tests.util.Print;

class ToolRunnerTests {

  @ParameterizedTest
  @ValueSource(strings = {"jar", "javac", "javadoc", "jlink"})
  void versionOfFoundationTool(String tool) {
    var runner = new ToolRunner();
    var response = runner.run(tool, "--version");
    assertSame(response, runner.history().getLast());
    assertTrue(response.isSuccessful());
    assertFalse(response.isError());
    assertTrue(response.toString().contains("" + Runtime.version().feature()), response.toString());
  }

  @Test
  void print() {
    var runner = new ToolRunner();
    runner.run("print", "10 PRINT 'HELLO WORLD'");
    runner.run("print", "20", "PRINT 20");
    runner.run(new Print("30 GOTO 10"));
    runner.run(new Print(true, "END.", 0));
    var actual = runner.history().stream().map(ToolResponse::out).collect(Collectors.joining("\n"));
    assertLinesMatch("""
        10 PRINT 'HELLO WORLD'
        20 PRINT 20
        30 GOTO 10
        END.
        """.lines(), actual.lines());
  }
}
