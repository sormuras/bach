package run.bach;

import run.duke.ToolFinders;

public record Toolbox(ModuleLayer layer, ToolFinders finders) {
  public static Toolbox EMPTY = new Toolbox(ModuleLayer.empty(), ToolFinders.EMPTY);

  public String toString(int indent) {
    var lines = new StringBuilder();
    lines.append("Toolbox [%d]".formatted(finders.list().size())).append('\n');
    for (var finder : finders) {
      var type = finder.getClass().getSimpleName();
      var description = finder.description();
      var identifiers = finder.identifiers();
      var size = identifiers.size();
      var s = size == 1 ? "" : "s";
      lines.append("  %s [%d tool%s] (%s)%n".formatted(description, size, s, type));
      for (var identifier : identifiers.stream().sorted().toList()) {
        lines.append("    %s%n".formatted(identifier));
      }
    }
    return lines.toString().indent(indent).stripTrailing();
  }
}
