package com.github.sormuras.bach.api;

import com.github.sormuras.bach.internal.Strings;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public record Tweak(Set<CodeSpace> spaces, String trigger, List<String> arguments) {

  public static Tweak of(String string) {
    var lines = string.lines().toList();
    var size = lines.size();
    if (size < 2) throw new IllegalArgumentException("Too few tweak lines in: " + string);
    var split = lines.get(0).split(",");
    var namesOfSpaces = Stream.of(split).map(name -> Strings.toEnum(CodeSpace.class, name)).toList();
    var spaces = Set.copyOf(namesOfSpaces);
    var trigger = lines.get(1);
    return new Tweak(spaces, trigger, size == 2 ? List.of() : lines.subList(2, size));
  }

  public boolean isForSpace(CodeSpace space) {
    return spaces.contains(space);
  }
}
