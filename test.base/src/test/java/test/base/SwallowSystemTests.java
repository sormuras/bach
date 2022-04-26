package test.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class SwallowSystemTests {

  @Test
  @SwallowSystem
  void empty(SwallowSystem.Streams streams) {
    assertEquals(0, streams.lines().count());
    assertEquals(0, streams.errors().count());
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
    assertLinesMatch(Stream.of("out"), streams.lines());
    assertLinesMatch(Stream.of("err"), streams.errors());
  }
}
