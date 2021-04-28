package com.github.sormuras.bach.api;

import java.util.List;

public record Tweaks(List<Tweak> list) {

  public List<String> arguments(String trigger) {
    return list.stream()
        .filter(tweak -> tweak.trigger().equals(trigger))
        .flatMap(tweak -> tweak.arguments().stream())
        .toList();
  }
}
