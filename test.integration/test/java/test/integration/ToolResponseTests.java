package test.integration;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.sormuras.bach.tool.ToolResponse;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class ToolResponseTests {
  @Test
  void nonZeroExitCodeYieldsRuntimeException() {
    var response = new ToolResponse("X", List.of(), 0, Duration.ZERO, -1, "", "");
    var exception = assertThrows(RuntimeException.class, response::checkSuccessful);
    assertLinesMatch("""
        X returned error code -1
            ToolResponse.+
        """.lines(), exception.getMessage().lines());
  }
}
