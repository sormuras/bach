package test.base.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CommandTests {
  @Test
  void createCommandForToolCalledJar() {
    var command = Command.of("jar")
        .with("--create")
        .with("--file", Path.of("foo.jar"))
        .with("--verbose")
        .with("--main-class", "foo.bar.Main")
        .with("-C", Path.of("classes"), ".");
    assertLinesMatch("""
        --create
        --file
        foo.jar
        --verbose
        --main-class
        foo.bar.Main
        -C
        classes
        .
        """.lines().toList(), command.toStrings());
  }

  @Test
  void createJarCall() {
    var jar = Jar.create("foo.jar")
        .with("--verbose")
        .withMainClass("foo.bar.Main")
        .withFile(Path.of("bar.jar"))
        .with("-C", Path.of("classes"), ".");

    assertLinesMatch("""
        --create
        --file
        bar.jar
        --verbose
        --main-class
        foo.bar.Main
        -C
        classes
        .
        """.lines().toList(), jar.toStrings());
    assertEquals(Path.of("classes"), jar.findFirstArgument("-C").orElseThrow().values().get(0));
    assertEquals(".", jar.findFirstArgument("-C").orElseThrow().values().get(1));
  }
}
