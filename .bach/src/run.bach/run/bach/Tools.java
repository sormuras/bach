package run.bach;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.TreeMap;

public record Tools(ToolFinders finders) {
  public Tool get(String string) {
    for (var finder : finders.list()) {
      var found = finder.findFirst(string);
      if (found.isEmpty()) continue;
      return found.get();
    }
    throw new RuntimeException("No such tool: " + string);
  }

  public String toString(int indent) {
    var joiner = new StringJoiner("\n");
    var width = 3;
    var tools = new ArrayList<Tool>();
    var nicks = new TreeMap<String, List<Tool>>();
    for (var finder : finders.list()) {
      for (var tool : finder.findAll()) {
        tools.add(tool);
        nicks.computeIfAbsent(tool.nick(), __ -> new ArrayList<>()).add(tool);
        var length = tool.nick().length();
        if (length > width) width = length;
      }
    }
    var format = "%" + width + "s %s";
    for (var entry : nicks.entrySet()) {
      var names = entry.getValue().stream().map(Tool::name).toList();
      joiner.add(String.format(format, entry.getKey(), names));
    }
    var size = tools.size();
    joiner.add("    %d tool%s".formatted(size, size == 1 ? "" : "s"));
    return joiner.toString().indent(indent).stripTrailing();
  }
}
