package run.duke;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * An iterable list of tool call instances.
 *
 * @param list the list of tool call instances
 */
public record ToolCalls(List<ToolCall> list) implements Iterable<ToolCall> {
  // args = ["jar", "--version", "+", "javac", "--version", ...]
  public static ToolCalls of(String... args) {
    return ToolCalls.of("+", true, List.of(args));
  }

  // args = ["jar", "--version", <delimiter>, "javac", "--version", ...]
  public static ToolCalls of(String delimiter, boolean trim, List<String> args) {
    if (args.isEmpty()) return new ToolCalls(List.of());
    var arguments = new ArrayDeque<>(args);
    var elements = new ArrayList<String>();
    var calls = new ArrayList<ToolCall>();
    while (true) {
      var empty = arguments.isEmpty();
      if (empty || arguments.peekFirst().equals(delimiter)) {
        calls.add(ToolCall.of(elements.get(0)).with(elements.stream().skip(1)));
        elements.clear();
        if (empty) break;
        arguments.pop(); // consume delimiter
      }
      var element = arguments.pop(); // consume element
      elements.add(trim ? element.trim() : element);
    }
    return new ToolCalls(List.copyOf(calls));
  }

  public ToolCalls {
    if (list == null) throw new IllegalArgumentException("list must not be null");
  }

  @Override
  public Iterator<ToolCall> iterator() {
    return list.iterator();
  }
}
