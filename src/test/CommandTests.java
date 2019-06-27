
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommandTests {
  @Test
  void toStringReturnsNameAndListOfArguments() {
    assertEquals("Command{name='empty', list=[<empty>]}", new Bach.Command("empty").toString());
    assertEquals("Command{name='test', list=['a', 'b']}", new Bach.Command("test", "a", "b").toString());
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
          "o"
        },
        new Bach.Command("noop")
            .addEach("a", "b")
            .addEach(List.of("c"))
            .add("d", "e")
            .add("f")
            .addIff(true, "g")
            .addIff(false, "X", "Y")
            .add("h", List.of(Path.of("i0"), Path.of("i1")))
            .add("j", List.of(Path.of("k")))
            .add("l", List.of(Path.of("m")), s -> "n=" + s)
            .addIff(true, args -> args.add("o"))
            .addIff(false, args -> args.add("Z"))
            .toStringArray());
  }
}
