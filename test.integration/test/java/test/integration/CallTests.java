package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.tool.AnyCall;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.StringJoiner;
import org.junit.jupiter.api.Test;

class CallTests {

  private static AnyCall call(String name, String... args) {
    return new AnyCall(name, List.of(args));
  }

  private static String line(ToolCall<?> call) {
    var joiner = new StringJoiner(" ");
    joiner.add(call.name());
    call.arguments().forEach(joiner::add);
    return joiner.toString();
  }

  @Test
  void withoutArguments() {
    var call = call("name");
    assertEquals("name", call.name());
    assertEquals("name", line(call));
    assertEquals(line(call("name")), line(call));
  }

  @Test
  void withArguments() {
    var call = call("name").with("1").with("2", 3).with("4", '5', 0x6);
    assertEquals("name", call.name());
    assertEquals("name 1 2 3 4 5 6", line(call));
    assertEquals("1 2 3[...]", call.toDescription(10));
  }

  @Test
  void withCollectionOfPathJoinedBySystemDependentPathSeparator() {
    var call = call("").with("1", List.of(Path.of("2"), Path.of("3")));
    assertEquals("1", call.arguments().get(0));
    assertEquals("2" + File.pathSeparator + "3", call.arguments().get(1));
  }

  @Test
  void withAllStrings() {
    var call = call("").withAll("1", "2", "3");
    assertEquals("1", call.arguments().get(0));
    assertEquals("2", call.arguments().get(1));
    assertEquals("3", call.arguments().get(2));
  }

  @Test
  void withAllObjects() {
    var call = call("").withAll("1", '2', 0x3);
    assertEquals("1", call.arguments().get(0));
    assertEquals("2", call.arguments().get(1));
    assertEquals("3", call.arguments().get(2));
  }

  @Test
  void withIfTrue() {
    var condition = new Random().nextBoolean();
    var command = call("").ifTrue(condition, c -> c.with("✔")).ifTrue(!condition, c -> c.with("❌"));
    assertEquals(condition ? "✔" : "❌", command.arguments().get(0));
  }

  @Test
  void withIfPresentOfOptional() {
    var present = Optional.of("✔");
    var empty = Optional.ofNullable(System.getProperty("❌"));
    var call = call("").ifPresent(present, ToolCall::with).ifPresent(empty, ToolCall::with);
    assertEquals("✔", call.arguments().get(0));
  }

  @Test
  void withIfPresentOfCollection() {
    var call = call("name").ifPresent(List.of("1", "2", "3"), ToolCall::withAll);
    assertEquals("name 1 2 3", line(call));
  }

  @Test
  void withForEach() {
    var call = call("name").forEach(List.of("1", "2", "3"), ToolCall::with);
    assertEquals("name 1 2 3", line(call));
  }

  @Test
  void resetArgumentsReturnsSameCommand() {
    var expected = call("");
    assertSame(expected, expected.arguments(expected.arguments()));
  }
}
