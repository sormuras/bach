package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Printer;
import com.github.sormuras.bach.api.Action;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class RunActionInEmptyDirectoryTests {

  @ParameterizedTest
  @EnumSource(Action.class)
  void action(Action action, @TempDir Path temp) throws Exception {
    var directory = Files.createDirectory(temp.resolve(action.name()));
    bach(0, ">>>>", "--chroot", directory, "--action", action.cli());
  }

  private static void bach(int expectedStatus, String expectedOutput, Object... objects) {
    var out = new StringWriter();
    var args = Stream.of(objects).map(Object::toString).toArray(String[]::new);
    var bach = Bach.of(Printer.of(new PrintWriter(out)), args);
    var status = bach.run();

    assertEquals(expectedStatus, status, () -> bach.logbook().toString());
    assertLinesMatch(expectedOutput.lines(), out.toString().lines());
  }
}
