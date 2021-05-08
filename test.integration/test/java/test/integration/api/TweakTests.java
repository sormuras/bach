package test.integration.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.api.CodeSpace;
import com.github.sormuras.bach.api.Tweak;
import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class TweakTests {
  @Test
  void empty() {
    var tweak = tweak("*");
    assertEquals("*", tweak.trigger());
    assertTrue(tweak.arguments().isEmpty());
  }

  @Test
  void withOneArgument() {
    var tweak = tweak("*", "1");
    assertEquals("*", tweak.trigger());
    assertEquals(List.of("1"), tweak.arguments());
  }

  @Test
  void cli() {
    var source = new Tweak(EnumSet.allOf(CodeSpace.class), "trigger", List.of("a", "b", "c"));
    var deque = new ArrayDeque<>(
        """
        main,test
        trigger
        3
        a
        b
        c
        """.lines().toList()
    );
    var target = Tweak.ofCommandLine(deque::removeFirst);
    assertEquals(source, target);
    assertTrue(target.isForSpace(CodeSpace.MAIN));
    assertTrue(target.isForSpace(CodeSpace.TEST));
  }

  private static Tweak tweak(String trigger, String... args) {
    return new Tweak(EnumSet.allOf(CodeSpace.class), trigger, List.of(args));
  }
}
