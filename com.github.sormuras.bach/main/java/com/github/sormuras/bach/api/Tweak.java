package com.github.sormuras.bach.api;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public record Tweak(Set<CodeSpace> spaces, String trigger, List<String> arguments) {

  public static Tweak of(ProjectInfo.Tweak info) {
    var arguments = new ArrayList<String>();
    arguments.add(info.option());
    arguments.addAll(List.of(info.value()));
    return new Tweak(Set.of(info.spaces()), info.trigger(), List.copyOf(arguments));
  }

  public static Tweak ofCommandLine(Supplier<String> supplier) {
    var namesOfSpaces = Stream.of(supplier.get().split(","));
    var spaces = EnumSet.copyOf(namesOfSpaces.map(CodeSpace::ofCli).toList());
    var trigger = supplier.get();
    var count = Integer.parseInt(supplier.get());
    var arguments = IntStream.range(0, count).mapToObj(__ -> supplier.get()).toList();
    return new Tweak(spaces, trigger, arguments);
  }

  public boolean isForSpace(CodeSpace space) {
    return spaces.contains(space);
  }
}
