package test.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ComposedPathMatcherTests {

  @Test
  void empty() {
    var empty = new ComposedPathMatcher(List.of());
    assertFalse(empty.matches(Path.of("")));
    assertFalse(empty.matches(Path.of("a")));
    assertEquals("ComposedPathMatcher[matchers=[]]", empty.toString());
  }

  @Test
  void streamSyntaxAndPatterns() {
    assertLinesMatch(
        """
        glob:1/suffix
        glob:2/suffix
        glob:3/suffix
        """.lines(),
        ComposedPathMatcher.stream("glob", "suffix", "1", "2", "3")
    );
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "f", "g"})
  void globMatchesFirstLevelPaths(String string) {
    var matcher = ComposedPathMatcher.of("glob", "", "*");
    assertTrue(matcher.matches(Path.of(string)));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "f", "d/f", "d/../f"})
  void globMatchesPathsOfAllLevels(String string) {
    var matcher = ComposedPathMatcher.of("glob", "", "**");
    assertTrue(matcher.matches(Path.of(string)));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "module-info.java",
        "m/module-info.java",
        "m/main/module-info.java",
        "m/main/java/module-info.java",
        "m/main/java-module/module-info.java"
      })
  void globMatchesMainPatterns(String string) {
    var patterns = List.of("module-info.java", "*", "**");
    var matcher = ComposedPathMatcher.ofGlobModules(patterns);
    assertTrue(matcher.matches(Path.of(string)));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "test/module-info.java",
        "m/test/module-info.java",
        "m/test/java/module-info.java",
        "m/test/java-module/module-info.java"
      })
  void globMatchesTestPatterns(String string) {
    var patterns = List.of("test", "**/test", "**/test/**");
    var matcher = ComposedPathMatcher.ofGlobModules(patterns);
    assertTrue(matcher.matches(Path.of(string)));
  }
}
