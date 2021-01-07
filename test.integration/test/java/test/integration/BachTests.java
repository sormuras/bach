package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Base;
import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.Recording;
import java.util.List;
import java.util.spi.ToolProvider;
import org.junit.jupiter.api.Test;

class BachTests {
  @Test
  void defaults() {
    var bach = new Bach();
    // components
    assertNotNull(bach.project());
    assertNotNull(bach.browser());
    assertTrue(bach.recordings().isEmpty());
    // compute
    assertTrue(bach.computeExternalModuleUri("java.base").isEmpty());
    assertEquals("foo@0.jar", bach.computeMainJarFileName("foo"));
    assertEquals("jar", bach.computeToolProvider("jar").name());
    assertEquals("javac", bach.computeToolProvider("javac").name());
    assertEquals("javadoc", bach.computeToolProvider("javadoc").name());
    var tools = List.of("jar", "javac", "javadoc", "javap", "jdeps", "jlink", "jmod", "jpackage");
    assertTrue(bach.computeToolProviders().map(ToolProvider::name).toList().containsAll(tools));
  }

  @Test
  void print() {
    var bach = new Bach(Base.ofSystem(), silent -> {});

    bach.run(Command.of("print").add("10", "PRINT 'HELLO WORLD'"));
    bach.run(ToolProvider.findFirst("print").orElseThrow(), List.of("20", "PRINT 20"));
    bach.run(new PrintToolProvider("30 GOTO 10"));
    bach.run(new PrintToolProvider(true, "END.", 0));

    assertLinesMatch(
        """
            10 PRINT 'HELLO WORLD'
            20 PRINT 20
            30 GOTO 10
            END.
            """
            .lines(),
        bach.recordings().stream().map(Recording::output));
  }
}
