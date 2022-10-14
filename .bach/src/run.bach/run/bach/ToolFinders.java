package run.bach;

import java.util.List;
import java.util.StringJoiner;

public record ToolFinders(List<ToolFinder> list) {

  public ToolFinders(List<ToolFinder> list) {
    this.list = List.copyOf(list);
  }

  public String toString(int indent) {
    var joiner = new StringJoiner("\n");
    for (var finder : list) {
      var description = finder.description();
      var tools = finder.findAll().size();
      joiner.add("%s [%d]".formatted(description, tools));
    }
    var size = list.size();
    joiner.add("    %d finder%s".formatted(size, size == 1 ? "" : "s"));
    return joiner.toString().indent(indent).stripTrailing();
  }
}
