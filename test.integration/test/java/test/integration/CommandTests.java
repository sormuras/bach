package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.github.sormuras.bach.Command;
import org.junit.jupiter.api.Test;

class CommandTests {

  @Test
  void withoutArguments() {
    var command = Command.of("name");
    assertEquals("name", command.name());
    var commandLine = command.toLine();
    assertEquals("name", commandLine);
    assertEquals(Command.of("name").toLine(), commandLine);
  }

  @Test
  void withArguments() {
    var command = Command.of("name").add("1").add("2", 3).add("4", '5', 0x6);
    assertEquals("name", command.name());
    assertEquals("name 1 2 3 4 5 6", command.toLine());
  }

  @Test
  void resetArgumentsReturnsSameCommand() {
    var expected = Command.of("name");
    assertSame(expected, expected.arguments(expected.arguments()));
  }

  @Test
  void touchFoundationTools() {
    assertEquals("jar", Command.jar().name());
    assertEquals("javac", Command.javac().name());
    assertEquals("javadoc", Command.javadoc().name());
    assertEquals("jdeps", Command.jdeps().name());
    assertEquals("jlink", Command.jlink().name());
  }
}
