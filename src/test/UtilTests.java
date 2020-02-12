// default package

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UtilTests {

  @Nested
  class Args {

    @Test
    void empty() {
      var args = new Bach.Util.Args();
      assertLinesMatch(List.of(), args.list());
      assertEquals("Args{}", args.toString());
      assertArrayEquals(new String[0], args.toStrings());
    }

    @Test
    void touch() {
      var args =
          new Bach.Util.Args()
              .add(1)
              .add("key", "value")
              .add(true, "first")
              .add(true, "second", "more")
              .add(false, "suppressed")
              .forEach(List.of('a', 'b', 'c'), Bach.Util.Args::add);
      assertLinesMatch(
          List.of("1", "key", "value", "first", "second", "more", "a", "b", "c"), args.list());
    }
  }
}
