package com.github.sormuras.bach;

import java.util.Set;
import java.util.stream.Stream;

public record Flags(Set<Flag> set) {
  public static Flags of(Flag... flags) {
    return new Flags(Set.of(flags));
  }

  public Flags with(Flag flag, boolean add) {
    if (!add) return this;
    return with(flag);
  }

  public Flags with(Flag... flags) {
    if (flags.length == 0) return this;
    return new Flags(Set.copyOf(Stream.concat(set.stream(), Stream.of(flags)).toList()));
  }
}
