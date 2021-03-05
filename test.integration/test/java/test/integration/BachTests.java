package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.Recording;
import com.github.sormuras.bach.lookup.LookupException;
import com.github.sormuras.bach.project.JavaStyle;
import com.github.sormuras.bach.project.Libraries;
import com.github.sormuras.bach.project.Tweak;
import java.util.List;
import java.util.spi.ToolProvider;
import org.junit.jupiter.api.Test;

class BachTests {

  private static Tweak newTweak(String trigger, String... arguments) {
    return new Tweak(trigger, List.of(arguments));
  }

  @Test
  void defaults() {
    var bach = new Bach(Options.of());
    // top-level "components"
    assertNotNull(bach.project());
    assertNotNull(bach.browser());
    assertTrue(bach.recordings().isEmpty());
    // default properties
    var project = bach.project();
    assertEquals("bach", project.name());
    assertEquals("0", project.version());
    assertEquals(Libraries.of(), project.libraries());
    assertEquals(JavaStyle.FREE, project.spaces().style());
    assertEquals("main", project.spaces().main().name());
    assertEquals(
        List.of(
            newTweak("javac", "-encoding", "UTF-8"),
            newTweak("javadoc", "-encoding", "UTF-8"),
            newTweak("jlink", "--compress", "2"),
            newTweak("jlink", "--no-header-files"),
            newTweak("jlink", "--no-man-pages"),
            newTweak("jlink", "--strip-debug")),
        project.spaces().main().tweaks().values());
    assertEquals("test", project.spaces().test().name());
    assertEquals(
        List.of(newTweak("javac", "-encoding", "UTF-8")),
        project.spaces().test().tweaks().values());
    // computations
    assertThrows(LookupException.class, () -> bach.computeExternalModuleUri("java.base"));
    assertEquals("foo@0.jar", bach.buildProjectJarFileName("foo"));
    assertEquals("jar", bach.computeToolProvider("jar").name());
    assertEquals("javac", bach.computeToolProvider("javac").name());
    assertEquals("javadoc", bach.computeToolProvider("javadoc").name());
    var tools = List.of("jar", "javac", "javadoc", "javap", "jdeps", "jlink", "jmod", "jpackage");
    assertTrue(bach.computeToolProviders().map(ToolProvider::name).toList().containsAll(tools));
  }

  @Test
  void print() {
    var bach = new Bach(Options.of("--silent"));

    bach.run(new PrintToolProvider("10 PRINT 'HELLO WORLD'"));
    bach.run(new PrintToolProvider(true, "20 GOTO 10", 0));

    assertLinesMatch(
        """
        10 PRINT 'HELLO WORLD'
        20 GOTO 10
        """.lines(),
        bach.recordings().stream().map(Recording::output));
  }
}
