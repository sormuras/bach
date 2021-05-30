package com.github.sormuras.bach.api;

import java.util.List;

public record Tweaks(List<Tweak> list) {

  public static Tweaks of(Tweak... tweaks) {
    return new Tweaks(List.of(tweaks));
  }

  public List<String> arguments(CodeSpace space, String trigger) {
    return list.stream()
        .filter(tweak -> tweak.isForSpace(space))
        .filter(tweak -> tweak.trigger().equals(trigger))
        .flatMap(tweak -> tweak.arguments().stream())
        .toList();
  }
}
