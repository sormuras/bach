package it;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.sormuras.bach.Command;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CommandTests {

  @Test
  void checkDefaultValues() {
    assertEquals("empty", new Command("empty").getName());
    assertEquals(List.of(), new Command("empty").getArguments());
    assertEquals("empty", new Command("empty").toCommandLine());
    assertEquals("empty", new Command("empty").toCommandLine("?"));
  }

  @Test
  void toCommandLineWithTabAsSeparator() {
    assertEquals("a\tb\tc", new Command("a", "b", "c").toCommandLine("\t"));
  }

  @Test
  void toStringReturnsNameAndListOfArguments() {
    assertEquals("Command{name='empty', args=[<empty>]}", new Command("empty").toString());
    assertEquals("Command{name='a', args=['b', 'c']}", new Command("a", "b", "c").toString());
  }

  @Test
  void addThemAll() {
    assertArrayEquals(
        new String[] {
          "a",
          "b",
          "c",
          "d",
          "e",
          "f",
          "g",
          "h",
          "i0" + File.pathSeparator + "i1",
          "j",
          "k",
          "l",
          "n=m",
          "o",
          "p",
          "q",
          "r",
          "s"
        },
        new Command("noop")
            .addEach("a", "b")
            .addEach(List.of("c"))
            .add("d", "e")
            .add("f")
            .addIff(true, "g")
            .addIff(false, "X", "Y")
            .add("h", List.of(Path.of("i0"), Path.of("i1")))
            .add("j", List.of(Path.of("k")))
            .add("l", List.of(Path.of("m")).stream(), s -> "n=" + s)
            .addIff(true, args -> args.add("o"))
            .addIff(false, args -> args.add("Z"))
            .addIff("p", Optional.of("q"))
            .addEach(List.of("r", "s"), Command::add)
            .toStringArray());
  }
}
