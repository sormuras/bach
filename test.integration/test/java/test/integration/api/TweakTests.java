package test.integration.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.api.Tweak;
import java.util.List;
import org.junit.jupiter.api.Test;

class TweakTests {
  @Test
  void empty() {
    var tweak = new Tweak("*", List.of());
    assertEquals("*", tweak.trigger());
    assertTrue(tweak.arguments().isEmpty());
  }

  @Test
  void withOneArgument() {
    var tweak = new Tweak("*", List.of("1"));
    assertEquals("*", tweak.trigger());
    assertEquals(List.of("1"), tweak.arguments());
  }
}
