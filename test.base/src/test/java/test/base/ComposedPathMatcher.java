package test.base;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A path matcher composed of path matchers.
 *
 * @param matchers the match operators to delegate to
 * @see java.nio.file.PathMatcher
 */
public record ComposedPathMatcher(List<PathMatcher> matchers) implements PathMatcher {

  public static ComposedPathMatcher of(String syntax, String suffix, String... patterns) {
    var system = FileSystems.getDefault();
    var matchers = stream(syntax, suffix, patterns).map(system::getPathMatcher).toList();
    return new ComposedPathMatcher(matchers);
  }

  public static ComposedPathMatcher ofGlobModules(List<String> patterns) {
    return ComposedPathMatcher.of("glob", "module-info.java", patterns.toArray(String[]::new));
  }

  public static Stream<String> stream(String syntax, String suffix, String... patterns) {
    return Stream.of(patterns)
        .map(pattern -> pattern.indexOf(':') > 0 ? pattern : syntax + ':' + pattern)
        .map(pattern -> pattern.endsWith(suffix) ? pattern : pattern + '/' + suffix);
  }

  @Override
  public boolean matches(Path path) {
    return anyMatch(path);
  }

  /**
   * {@return whether any matcher's pattern matches the given path}
   *
   * @param path the path to test
   * @see java.util.stream.Stream#anyMatch(Predicate)
   */
  public boolean anyMatch(Path path) {
    return matchers.stream().anyMatch(matcher -> matcher.matches(path));
  }
}
