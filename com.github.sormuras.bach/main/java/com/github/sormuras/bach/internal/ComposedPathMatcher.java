package com.github.sormuras.bach.internal;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.function.Predicate;

/**
 * A path matcher composed of path matchers.
 *
 * @param matchers the match operators to delegate to
 * @see PathMatcher
 */
public record ComposedPathMatcher(List<PathMatcher> matchers) implements PathMatcher {

  public static ComposedPathMatcher of(String syntax, String suffix, String... patterns) {
    return new ComposedPathMatcher(
        List.of(patterns).stream()
            .map(pattern -> pattern.indexOf(':') > 0 ? pattern : syntax + ':' + pattern)
            .map(pattern -> pattern.endsWith(suffix) ? pattern : pattern + '/' + suffix)
            .map(syntaxAndPattern -> FileSystems.getDefault().getPathMatcher(syntaxAndPattern))
            .toList());
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
