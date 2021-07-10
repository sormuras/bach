package com.github.sormuras.bach;

import com.github.sormuras.bach.call.ToolCall;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public interface Call {

  static ToolCall tool(String name, Object... arguments) {
    return new ToolCall(name, List.of()).withAll(arguments);
  }

  static Tree tree(String caption, Call... calls) {
    return new Tree(caption, false, List.of(calls), List.of());
  }

  static Tree tree(String caption, Stream<Call> calls) {
    return new Tree(caption, calls.isParallel(), calls.toList(), List.of());
  }

  String name();

  List<String> arguments();

  default String toDescription(int maxLineLength) {
    var arguments = arguments();
    var line = arguments.isEmpty() ? "</>" : String.join(" ", arguments);
    return line.length() <= maxLineLength ? line : line.substring(0, maxLineLength - 5) + "[...]";
  }

  record Tree(String caption, boolean parallel, List<Call> calls, List<Tree> trees) {

    public boolean isEmpty() {
      return calls.isEmpty() && trees.isEmpty();
    }

    public Tree with(Call call, Call... more) {
      var calls = new ArrayList<>(this.calls);
      calls.add(call);
      calls.addAll(List.of(more));
      return new Tree(caption, parallel, calls, trees);
    }

    public Tree with(Tree tree, Tree... more) {
      var trees = new ArrayList<>(this.trees);
      trees.add(tree);
      trees.addAll(List.of(more));
      return new Tree(caption, parallel, calls, trees);
    }
  }
}
