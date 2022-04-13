import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SwallowSystemTests {

  @Test
  @SwallowSystem
  void empty(SwallowSystem.Streams streams) {
    assertTrue(streams.lines().isEmpty());
    assertTrue(streams.errors().isEmpty());
  }

  @Test
  @SwallowSystem
  void normalAndErrorOutput() {
    System.out.println("out");
    System.err.println("err");
  }

  @Test
  @SwallowSystem
  void normalAndErrorOutput(SwallowSystem.Streams streams) {
    System.out.println("out");
    System.err.println("err");
    assertLinesMatch(List.of("out"), streams.lines());
    assertLinesMatch(List.of("err"), streams.errors());
  }
}
