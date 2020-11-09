package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.github.sormuras.bach.tool.Command;
import org.junit.jupiter.api.Test;

class CommandTests {

  @Test
  void withoutArguments() {
    var command = Command.of("name");
    assertEquals("name", command.name());
    var commandLine = command.toString();
    assertEquals("name", commandLine);
    assertEquals(Command.builder("name").build().toString(), commandLine);
  }

  @Test
  void withArguments() {
    var command = Command.builder("name").with(1).with("2", 3, 4).build();
    assertEquals("name", command.name());
    assertEquals("name 1 2 3 4", command.toString());
    assertEquals(Command.of("name", 1, 2, 3, 4), command);
  }

  @Test
  void toCommandReturnsItself() {
    var expected = Command.of("name");
    assertSame(expected, expected.toCommand());
  }
}
