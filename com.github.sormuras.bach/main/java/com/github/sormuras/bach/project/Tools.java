package com.github.sormuras.bach.project;

import java.util.Set;

/**
 * Tool-related settings.
 *
 * @param limits the names of tools that limit the universe of executable tools
 * @param skips the names of tools that must not be executed
 */
public record Tools(Set<String> limits, Set<String> skips) {

  /** {@return {@code true} if the given tool is within the configured limits and not skipped} */
  public boolean enabled(String tool) {
    return limit(tool) && !skip(tool);
  }

  /** {@return {@code true} if the given tool is within the universe of executable tools} */
  public boolean limit(String tool) {
    return limits.isEmpty() || limits.contains(tool);
  }

  /** {@return {@code true} if the given tool must not be executed} */
  public boolean skip(String tool) {
    return !skips.isEmpty() && skips.contains(tool);
  }
}
