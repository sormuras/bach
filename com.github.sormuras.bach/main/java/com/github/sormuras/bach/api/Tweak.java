package com.github.sormuras.bach.api;

import com.github.sormuras.bach.internal.Strings;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public record Tweak(Set<CodeSpace> spaces, String trigger, List<String> arguments) {

  public static Tweak of(ProjectInfo.Tweak info) {
    var spaces = Set.of(info.spaces());
    var trigger = info.tool();
    var arguments = new ArrayList<String>();
    Strings.unroll(info.with()).forEach(arguments::add);
    Strings.unroll(info.more()).forEach(arguments::add);
    return new Tweak(spaces, trigger, List.copyOf(arguments));
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
