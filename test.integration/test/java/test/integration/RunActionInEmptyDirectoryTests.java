package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Printer;
import com.github.sormuras.bach.api.Action;
import com.github.sormuras.bach.api.Option;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class RunActionInEmptyDirectoryTests {

  @ParameterizedTest
  @EnumSource(Action.class)
  void action(Action action, @TempDir Path temp) {
    bach(0, ">>>>", Option.CHROOT, temp, Option.ACTION, action);
  }

  @Test
  void unsupported(@TempDir Path temp) {
    bach(1, """
        Bach .+
        >>>>
        Exception: com.github.sormuras.bach.api.UnsupportedActionException: Unsupported action: XYZ
        >>>>
        """, Option.CHROOT, temp, Option.ACTION, "XYZ");
  }

  private static void bach(int expectedStatus, String expectedOutput, Object... objects) {
    var out = new StringWriter();
    var args =
        Stream.of(objects)
            .map(
                object -> {
                  if (object instanceof Option option) return option.cli();
                  if (object instanceof Action action) return action.cli();
                  return object.toString();
                })
            .toArray(String[]::new);
    var bach = Bach.of(Printer.of(new PrintWriter(out)), args);
    var status = bach.run();

    assertEquals(expectedStatus, status, () -> bach.logbook().toString());
    assertLinesMatch(expectedOutput.lines(), out.toString().lines());
  }
}
