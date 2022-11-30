package run.duke;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public record ToolCalls(List<ToolCall> list) implements Iterable<ToolCall> {
  public static ToolCalls of(String... args) {
    return ToolCalls.of(List.of(args));
  }

  public static ToolCalls of(List<String> args) {
    if (args.isEmpty()) return new ToolCalls(List.of());
    var arguments = new ArrayDeque<>(args);
    var elements = new ArrayList<String>();
    var calls = new ArrayList<ToolCall>();
    while (true) {
      var empty = arguments.isEmpty();
      if (empty || arguments.peekFirst().equals("+")) {
        calls.add(new ToolCall(elements.get(0), elements.stream().skip(1).toList()));
        elements.clear();
        if (empty) break;
        arguments.pop(); // consume delimiter
      }
      elements.add(arguments.pop().trim()); // consume trimmed element
    }
    return new ToolCalls(List.copyOf(calls));
  }

  public ToolCalls(ToolCall... calls) {
    this(List.of(calls));
  }

  public boolean isEmpty() {
    return list.isEmpty();
  }

  @Override
  public Iterator<ToolCall> iterator() {
    return list.iterator();
  }
}
