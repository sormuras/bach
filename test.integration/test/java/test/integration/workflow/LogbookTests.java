package test.integration.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.workflow.Run;
import com.github.sormuras.bach.workflow.Logbook;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.System.Logger.Level;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LogbookTests {

  @Test
  void log() {
    var discard = new PrintWriter(Writer.nullWriter());
    var logbook = Logbook.of(discard, discard, true);
    assertEquals(0, logbook.messages().size());
    assertEquals(0, logbook.runs().size());
    assertEquals(0, logbook.exceptions().size());
    logbook.log(Level.TRACE, "Message: " + Level.TRACE);
    logbook.log(Level.DEBUG, "Message: " + Level.DEBUG);
    logbook.log(Level.INFO, "Message: " + Level.INFO);
    logbook.log(Level.WARNING, "Message: " + Level.WARNING);
    logbook.log(Level.ERROR, "Message: " + Level.ERROR);
    logbook.log(newRun(0, "tool", "1", "2", "3"));
    logbook.log(new Exception("123"));
    assertEquals(7, logbook.messages().size());
    assertEquals(1, logbook.runs().size());
    assertEquals(1, logbook.exceptions().size());
    assertLinesMatch(
        """
        Message: TRACE
        Message: DEBUG
        Message: INFO
        Message: WARNING
        Message: ERROR
        Run: Run[name=tool, args=[1, 2, 3], thread=123, duration=PT0S, code=0, output=out, errors=err]
        Exception: java.lang.Exception: 123
        """
            .lines(),
        logbook.lines());
  }

  @Nested
  class RunTests {

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, -1, 1, 2, 3, Integer.MAX_VALUE})
    void nonZeroReturnCodeIsAnError(int code) {
      var run = newRun(code, "tool");
      assertTrue(run.isError());
      var exception = assertThrows(Exception.class, run::requireSuccessful);
      assertLinesMatch(
          """
          tool returned code %d
          >> DETAILS >>
          """
              .formatted(code)
              .lines(),
          exception.getMessage().lines());
    }

    @Test
    void zeroReturnCodeIsSuccessful() {
      var run = newRun(0, "tool");
      assertTrue(run.isSuccessful());
    }
  }

  static Run newRun(int code, String tool, String... args) {
    return new Run(tool, List.of(args), 123L, Duration.ZERO, code, "out", "err");
  }
}
