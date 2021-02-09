package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.Command;
import org.junit.jupiter.api.Test;

class CommandTests {

  @Test
  void withoutArguments() {
    var command = Command.of("name");
    assertEquals("name", command.name());
    assertTrue(command.findFirstArgument("").isEmpty());
    var commandLine = command.toLine();
    assertEquals("name", commandLine);
    assertEquals(Command.of("name").toLine(), commandLine);
  }

  @Test
  void withArguments() {
    var command = Command.of("name").add("1").add("2", 3, 4);
    assertEquals("name", command.name());
    assertTrue(command.findFirstArgument("").isEmpty());
    assertTrue(command.findFirstArgument("1").isPresent());
    assertTrue(command.findFirstArgument("2").isPresent());
    assertTrue(command.findFirstArgument("3").isEmpty());
    assertTrue(command.findFirstArgument("4").isEmpty());
    assertEquals("name 1 2 3 4", command.toLine());
    assertTrue(command.clear("1").clear("2").arguments().isEmpty());
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
