package com.github.sormuras.bach.api;

import java.util.Set;

/**
 * Tool-related settings.
 *
 * @param limits the names of tools that limit the universe of executable tools
 * @param skips the names of tools that must not be executed
 * @param tweaks additional arguments per code space and trigger, usually a tool name
 */
public record Tools(Set<String> limits, Set<String> skips, Tweaks tweaks) {

  public static Tools of(String... limits) {
    return new Tools(Set.of(limits), Set.of(), Tweaks.of());
  }

  public static Tools of(ProjectInfo info) {
    var limits = Set.of(info.tool().limit());
    var skips = Set.of(info.tool().skip());
    var tweaks = Tweaks.of(info);
    return new Tools(limits, skips, tweaks);
  }

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
