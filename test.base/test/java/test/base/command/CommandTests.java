package test.base.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CommandTests {
  @Test
  void callJar() {
    var command =
        Command.call("jar")
            .add("--create")
            .add("--file", Path.of("foo.jar"))
            .add("--verbose")
            .add("--main-class", "foo.bar.Main")
            .add("-C", Path.of("classes"), ".");

    assertEquals("jar", command.tool());
    assertEquals(Path.of("classes"), command.findFirstArgument("-C").orElseThrow().values().get(0));
    assertEquals(".", command.findFirstArgument("-C").orElseThrow().values().get(1));
    assertLinesMatch(
        """
        --create
        --file
        foo.jar
        --verbose
        --main-class
        foo.bar.Main
        -C
        classes
        .
        """
            .lines()
            .toList(),
        command.toStrings());
  }

  @Test
  void createJar() {
    var jar =
        Jar.create("foo.jar")
            .add("--verbose")
            .addMainClass("foo.bar.Main")
            .file(Path.of("bar.jar"))
            .add("-C", Path.of("classes"), ".")
            .add("-C", Path.of("others"), ".")
            .clear("--verbose")
            .mode(Jar.Mode.DESCRIBE_MODULE)
            .clear("-C")
            .clear("--main-class");

    assertEquals("jar", jar.tool());
    assertLinesMatch(
        """
        --describe-module
        --file
        bar.jar
        """
            .lines()
            .toList(),
        jar.toStrings());
  }
}
