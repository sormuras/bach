package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.ToolRun;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.spi.ToolProvider;
import org.junit.jupiter.api.Test;

class ToolProviderTests {

  static ToolRun run(ToolCall<?> call) {
    var out = new StringWriter();
    var err = new StringWriter();
    var start = Instant.now();
    var tool = ToolProvider.findFirst(call.name()).orElseThrow();
    var args = call.arguments().toArray(String[]::new);
    var code = tool.run(new PrintWriter(out, true), new PrintWriter(err, true), args);
    return new ToolRun(
        call.name(),
        call.arguments(),
        Thread.currentThread().getId(),
        Duration.between(start, Instant.now()),
        code,
        out.toString(),
        err.toString());
  }

  @Test
  void version() {
    var run = run(ToolCall.of("bach").with("--version"));
    assertEquals(0, run.code(), run.toString());
  }
}
