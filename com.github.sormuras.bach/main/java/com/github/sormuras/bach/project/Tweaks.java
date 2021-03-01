package com.github.sormuras.bach.project;

import java.util.List;

/** A list of tweaks. */
public record Tweaks(List<Tweak> values) {

  /**
   * {@return a possibly empty list of additional arguments for the given trigger}
   *
   * @param trigger the tweak trigger
   */
  public List<String> arguments(String trigger) {
    return values.stream()
        .filter(tweak -> tweak.trigger().equals(trigger))
        .flatMap(tweak -> tweak.arguments().stream())
        .toList();
  }
}
