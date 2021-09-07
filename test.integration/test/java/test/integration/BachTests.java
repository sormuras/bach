package test.integration;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Configuration;
import org.junit.jupiter.api.Test;

class BachTests {

  @Test
  void version() {
    assertNotNull(Bach.version());
  }

  @Test
  void log() {
    var bach =
        new Bach(
            Configuration.of(
                Configuration.Pathing.ofCurrentWorkingDirectory(),
                Configuration.Printing.ofErrorsOnly()));
    bach.logCaption("caption");
    bach.logMessage("message");
    bach.logMessage(System.Logger.Level.TRACE, "trace");
  }
}
