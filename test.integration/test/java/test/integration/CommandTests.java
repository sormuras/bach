package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.github.sormuras.bach.Command;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Random;
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
    var command = Command.of("name").with("1").with("2", 3).with("4", '5', 0x6);
    assertEquals("name", command.name());
    assertEquals("name 1 2 3 4 5 6", command.toLine());
    assertEquals("1 2 3[...]", command.toDescription(10));
  }

  @Test
  void withCollectionOfPathJoinedBySystemDependentPathSeparator() {
    var command = Command.of("").with("1", List.of(Path.of("2"), Path.of("3")));
    assertEquals("1", command.arguments().get(0));
    assertEquals("2" + File.pathSeparator + "3", command.arguments().get(1));
  }

  @Test
  void withAllStrings() {
    var command = Command.of("").withAll("1", "2", "3");
    assertEquals("1", command.arguments().get(0));
    assertEquals("2", command.arguments().get(1));
    assertEquals("3", command.arguments().get(2));
  }

  @Test
  void withAllObjects() {
    var command = Command.of("").withAll("1", '2', 0x3);
    assertEquals("1", command.arguments().get(0));
    assertEquals("2", command.arguments().get(1));
    assertEquals("3", command.arguments().get(2));
  }

  @Test
  void withIfTrue() {
    var condition = new Random().nextBoolean();
    var command =
        Command.of("").ifTrue(condition, c -> c.with("✔")).ifTrue(!condition, c -> c.with("❌"));
    assertEquals(condition ? "✔" : "❌", command.arguments().get(0));
  }

  @Test
  void withIfPresentOfOptional() {
    var present = Optional.of("✔");
    var empty = Optional.ofNullable(System.getProperty("❌"));
    var command = Command.of("").ifPresent(present, Command::with).ifPresent(empty, Command::with);
    assertEquals("✔", command.arguments().get(0));
  }

  @Test
  void withIfPresentOfCollection() {
    var command = Command.of("name").ifPresent(List.of("1", "2", "3"), Command::withAll);
    assertEquals("name 1 2 3", command.toLine());
  }

  @Test
  void withForEach() {
    var command = Command.of("name").forEach(List.of("1", "2", "3"), Command::with);
    assertEquals("name 1 2 3", command.toLine());
  }

  @Test
  void resetArgumentsReturnsSameCommand() {
    var expected = Command.of("");
    assertSame(expected, expected.arguments(expected.arguments()));
  }
}
