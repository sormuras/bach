package test.integration.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.api.Tweak;
import com.github.sormuras.bach.api.Tweaks;
import java.util.List;
import org.junit.jupiter.api.Test;

class TweaksTests {
  @Test
  void empty() {
    var tweaks = new Tweaks(List.of());
    assertTrue(tweaks.list().isEmpty());
    assertTrue(tweaks.arguments("*").isEmpty());
  }

  @Test
  void withOneTweak() {
    var one = new Tweak("*", List.of("1"));
    var tweaks = new Tweaks(List.of(one));
    assertEquals(1, tweaks.list().size());
    assertEquals(List.of("1"), tweaks.arguments("*"));
  }

  @Test
  void withTwoTweak() {
    var one = new Tweak("*", List.of("1"));
    var two = new Tweak("*", List.of("2"));
    var tweaks = new Tweaks(List.of(one, two));
    assertEquals(2, tweaks.list().size());
    assertEquals(List.of("1", "2"), tweaks.arguments("*"));
  }
}
