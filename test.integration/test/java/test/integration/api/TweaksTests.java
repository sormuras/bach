package test.integration.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.api.CodeSpace;
import com.github.sormuras.bach.api.Tweak;
import com.github.sormuras.bach.api.Tweaks;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class TweaksTests {
  @Test
  void empty() {
    var tweaks = Tweaks.of();
    assertTrue(tweaks.list().isEmpty());
    assertTrue(tweaks.arguments(CodeSpace.MAIN, "*").isEmpty());
    assertTrue(tweaks.arguments(CodeSpace.MAIN, "*").isEmpty());
  }

  @Test
  void withOneTweak() {
    var tweaks = Tweaks.of(tweak("*", "1"));
    assertEquals(1, tweaks.list().size());
    assertEquals(List.of("1"), tweaks.arguments(CodeSpace.MAIN, "*"));
    assertEquals(List.of("1"), tweaks.arguments(CodeSpace.TEST, "*"));
  }

  @Test
  void withTwoTweak() {
    var one = tweak("*", "1");
    var two = tweak("*", "2");
    var tweaks = Tweaks.of(one, two);
    assertEquals(2, tweaks.list().size());
    assertEquals(List.of("1", "2"), tweaks.arguments(CodeSpace.MAIN, "*"));
    assertEquals(List.of("1", "2"), tweaks.arguments(CodeSpace.TEST, "*"));
  }

  private static Tweak tweak(String trigger, String... args) {
    return new Tweak(EnumSet.allOf(CodeSpace.class), trigger, List.of(args));
  }
}
