package tests;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.sormuras.bach.ToolResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ToolResponseTests {
  @Test
  void nonZeroExitCodeYieldsRuntimeException() {
    var response = new ToolResponse("0", new String[0], 0, Duration.ZERO, -1, "", "");
    assertThrows(RuntimeException.class, response::checkSuccessful);
  }
}
