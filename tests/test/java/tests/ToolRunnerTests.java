package tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.ToolRunner;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
}
