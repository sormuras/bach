package run.bach;

import java.util.Arrays;
import java.util.List;

public record ToolCall(String name, List<String> arguments) {
  // line = "tool-name [tool-args...]"
  public static ToolCall ofLine(String line) {
    var args = line.trim().split("\\s+");
    if (args.length == 0) throw new IllegalArgumentException("No tool name in: " + line);
    var name = args[0];
    if (args.length == 1) return new ToolCall(name, List.of());
    if (args.length == 2) return new ToolCall(name, List.of(args[1]));
    if (args.length == 3) return new ToolCall(name, List.of(args[1], args[2]));
    return new ToolCall(name, List.of(Arrays.copyOfRange(args, 1, args.length)));
  }
}
