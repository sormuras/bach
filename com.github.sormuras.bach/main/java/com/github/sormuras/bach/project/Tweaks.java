package com.github.sormuras.bach.project;

import java.util.List;
import java.util.Map;

/**
 * A collection of tweak objects.
 *
 * @param map the tweaks
 */
public record Tweaks(Map<String, Tweak> map) {
  /**
   * Returns a possibly empty list of additional arguments for the given trigger.
   *
   * @param trigger the tweak trigger
   * @return a possibly empty list of additional arguments
   */
  public List<String> arguments(String trigger) {
    var tweak = map.get(trigger);
    return tweak == null ? List.of() : tweak.arguments();
  }
}
