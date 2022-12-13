package run.duke;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public record ToolFinders(List<ToolFinder> list) implements ToolFinder {
  public static final ToolFinders EMPTY = new ToolFinders();

  public ToolFinders() {
    this(List.of());
  }

  @Override
  public Optional<String> description() {
    var size = list.size();
    return Optional.of("ToolFinders [%d finder%s]".formatted(size, size == 1 ? "" : "s"));
  }

  @Override
  public Collection<? extends Tool> findTools() {
    return list.stream().flatMap(finder -> finder.findTools().stream()).toList();
  }

  @Override
  public Optional<? extends Tool> findTool(String string) {
    for (var finder : list) {
      var tool = finder.findTool(string);
      if (tool.isPresent()) return tool;
    }
    return Optional.empty();
  }

  public ToolFinders with(ToolFinder finder, ToolFinder... more) {
    var finders = new ArrayList<>(list);
    finders.add(finder);
    if (more.length > 0) finders.addAll(List.of(more));
    return new ToolFinders(List.copyOf(finders));
  }

  public ToolFinders with(Iterable<ToolFinder> more) {
    var finders = new ArrayList<>(list);
    for (var finder : more) finders.add(finder);
    if (list.size() == finders.size()) return this;
    return new ToolFinders(List.copyOf(finders));
  }
}
