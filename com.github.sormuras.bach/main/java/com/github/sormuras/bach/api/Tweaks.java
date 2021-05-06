package com.github.sormuras.bach.api;

import java.util.List;
import java.util.stream.Stream;

public record Tweaks(List<Tweak> list) {

  public static Tweaks of(Tweak... tweaks) {
    return new Tweaks(List.of(tweaks));
  }

  public static Tweaks of(ProjectInfo info) {
    return new Tweaks(Stream.of(info.tool().tweaks()).map(Tweak::of).toList());
  }

  public List<String> arguments(String trigger) {
    return list.stream()
        .filter(tweak -> tweak.trigger().equals(trigger))
        .flatMap(tweak -> tweak.arguments().stream())
        .toList();
  }
}
