package run.duke;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public record ToolCalls(String name, List<ToolCall> list) implements Iterable<ToolCall> {
  // args = ["jar", "--version", "+", "javac", "--version", ...]
  public static ToolCalls of(String name, String... args) {
    return ToolCalls.of(name, "+", true, List.of(args));
  }

  // args = ["jar", "--version", <delimiter>, "javac", "--version", ...]
  public static ToolCalls of(String name, String delimiter, boolean trim, List<String> args) {
    if (args.isEmpty()) return new ToolCalls(name, List.of());
    var arguments = new ArrayDeque<>(args);
    var elements = new ArrayList<String>();
    var calls = new ArrayList<ToolCall>();
    while (true) {
      var empty = arguments.isEmpty();
      if (empty || arguments.peekFirst().equals(delimiter)) {
        calls.add(new ToolCall(elements.get(0), elements.stream().skip(1).toList()));
        elements.clear();
        if (empty) break;
        arguments.pop(); // consume delimiter
      }
      var element = arguments.pop(); // consume element
      elements.add(trim ? element.trim() : element);
    }
    return new ToolCalls(name, List.copyOf(calls));
  }

  public ToolCalls {
    if (name == null) throw new IllegalArgumentException("name must not be null");
    if (list == null) throw new IllegalArgumentException("list must not be null");
  }

  @Override
  public Iterator<ToolCall> iterator() {
    return list.iterator();
  }
}
