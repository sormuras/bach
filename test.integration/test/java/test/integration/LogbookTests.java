package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.Logbook;
import org.junit.jupiter.api.Test;

class LogbookTests {

  @Test
  void exception() {
    var logbook = Logbook.of();
    assertEquals(0, logbook.exceptions().size());
    logbook.log(new Exception("123"));
    assertEquals(1, logbook.exceptions().size());
    assertLinesMatch(
        """
        Exception: java.lang.Exception: 123
        """.lines(), logbook.lines());
  }
}
