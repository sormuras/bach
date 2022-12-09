package run.bach;

import run.duke.ToolFinders;

public record Toolbox(ModuleLayer layer, ToolFinders finders) {
  public static Toolbox EMPTY = new Toolbox(ModuleLayer.empty(), ToolFinders.EMPTY);
}
