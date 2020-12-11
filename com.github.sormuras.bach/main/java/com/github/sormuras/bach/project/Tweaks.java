package com.github.sormuras.bach.project;

import java.util.List;

/**
 * A collection of tweak objects.
 *
 * @param list the tweaks
 */
public record Tweaks(List<Tweak> list) {
  /**
   * Returns a possibly empty list of additional arguments for the given trigger.
   *
   * @param trigger the tweak trigger
   * @return a possibly empty list of additional arguments
   */
  public List<String> arguments(String trigger) {
    return list.stream()
        .filter(tweak -> tweak.trigger().equals(trigger))
        .flatMap(tweak -> tweak.arguments().stream())
        .toList();
  }
}
