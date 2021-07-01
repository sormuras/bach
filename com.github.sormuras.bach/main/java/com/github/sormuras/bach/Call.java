package com.github.sormuras.bach;

import java.util.List;
import java.util.stream.Stream;

public interface Call {

  String name();

  List<String> arguments();

  default String toDescription(int maxLineLength) {
    var arguments = arguments();
    var line = arguments.isEmpty() ? "</>" : String.join(" ", arguments);
    return line.length() <= maxLineLength ? line : line.substring(0, maxLineLength - 5) + "[...]";
  }

  record Tree(String caption, Stream<? extends Call> calls, Stream<Tree> trees) {}

  static Tree tree(String caption) {
    return new Tree(caption, Stream.of(), Stream.of());
  }

  static Tree tree(String caption, Stream<? extends Call> calls) {
    return new Tree(caption, calls, Stream.of());
  }

  static Tree tree(String caption, Call call, Tree... trees) {
    return new Tree(caption, Stream.of(call), Stream.of(trees));
  }
}
