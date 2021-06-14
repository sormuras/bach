package test.base;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class FilesTests {

  record AnyPathMatcher(PathMatcher... matchers) implements Predicate<Path> {

    public static AnyPathMatcher of(String... patterns) {
      return of("glob", "", Arrays.stream(patterns));
    }

    public static AnyPathMatcher ofFiles(String fileName, String... patterns) {
      return of("glob", '/' + fileName, Arrays.stream(patterns));
    }

    public static AnyPathMatcher of(String syntax, String suffix, Stream<String> patterns) {
      return new AnyPathMatcher(
          patterns
              .map(pattern -> pattern.indexOf(':') > 0 ? pattern : syntax + ':' + pattern)
              .map(pattern -> pattern.endsWith(suffix) ? pattern : pattern + suffix)
              .map(syntaxAndPattern -> FileSystems.getDefault().getPathMatcher(syntaxAndPattern))
              .toArray(PathMatcher[]::new));
    }

    @Override
    public boolean test(Path path) {
      for (var matcher : matchers) if (matcher.matches(path)) return true;
      return false;
    }
  }

  @Nested
  class PathMatcherTests {

    @SuppressWarnings("unused")
    static List<Path> paths() {
      return List.of(
          // build program
          Path.of("bach/build.java"),
          // main module
          Path.of("com.github.sormuras.bach/main/java/module-info.java"),
          // test modules
          Path.of("com.github.sormuras.bach/test/java-module/module-info.java"),
          Path.of("test.base/test/java/module-info.java"),
          Path.of("test.integration/test/java/module-info.java"),
          Path.of("test.projects/test/java/module-info.java"),
          // sample project modules
          Path.of("test.projects/JigsawQuickStartGreetings/com.greetings/module-info.java"),
          Path.of("test.projects/JigsawQuickStartWorld/com.greetings/module-info.java"),
          Path.of("test.projects/JigsawQuickStartWorld/org.astro/module-info.java"),
          Path.of("test.projects/Simple/module-info.java"),
          Path.of("test.projects/Simplicissimus/module-info.java"));
    }

    @ParameterizedTest
    @MethodSource("paths")
    void isRegularFile(Path path) {
      assertTrue(Files.isRegularFile(path), path.toString());
    }

    @ParameterizedTest
    @MethodSource("paths")
    void matchesGlobArbitrary(Path path) {
      var matcher = path.getFileSystem().getPathMatcher("glob:**/*");
      assertTrue(matcher.matches(path));
    }

    @ParameterizedTest
    @MethodSource("paths")
    void matchesRegexArbitrary(Path path) {
      var matcher = path.getFileSystem().getPathMatcher("regex:.*");
      assertTrue(matcher.matches(path));
    }

    @ParameterizedTest
    @MethodSource("paths")
    void matchesGlobExactly(Path path) {
      var matcher = path.getFileSystem().getPathMatcher("glob:" + slashed(path));
      assertTrue(matcher.matches(path));
    }

    @Test
    void findTestModulesOfBachUsingSingleGlobPattern() {
      var matcher = FileSystems.getDefault().getPathMatcher("glob:**/test/**/module-info.java");
      assertTestModulesOfBach(matcher::matches);
    }

    @Test
    void findTestModulesOfBachUsingMultiPathMatcher() {
      var matcher = AnyPathMatcher.ofFiles("module-info.java", "*/test/java", "*/test/java-module");
      assertTestModulesOfBach(matcher);
    }

    void assertTestModulesOfBach(Predicate<Path> predicate) {
      assertLinesMatch(
          """
          com.github.sormuras.bach/test/java-module/module-info.java
          test.base/test/java/module-info.java
          test.integration/test/java/module-info.java
          test.projects/test/java/module-info.java
          """
              .lines(),
          paths().stream().filter(predicate).map(FilesTests::slashed));
    }
  }

  static String slashed(Path path) {
    return path.toString().replace('\\', '/');
  }
}
