package run.duke;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.spi.ToolProvider;

public record ToolFinders(List<ToolFinder> list) implements Iterable<ToolFinder>, ToolFinder {
  public static final ToolFinders EMPTY = new ToolFinders();

  public ToolFinders() {
    this(List.of());
  }

  @Override
  public Iterator<ToolFinder> iterator() {
    return list.iterator();
  }

  @Override
  public String description() {
    return "ToolFinders [%d finder%s]".formatted(list.size(), list.size() == 1 ? "" : "s");
  }

  public int size() {
    int size = 0;
    for (var finder : list) size += finder.identifiers().size();
    return size;
  }

  public Optional<Tool> find(String name, ToolRunner runner) {
    for (var finder : list) {
      var tool = finder.find(name, runner);
      if (tool.isPresent()) return tool;
    }
    return Optional.empty();
  }

  public ToolFinders with(ToolFinder finder, ToolFinder... more) {
    var finders = new ArrayList<>(this.list);
    finders.add(finder);
    if (more.length > 0) finders.addAll(List.of(more));
    return new ToolFinders(List.copyOf(finders));
  }

  public ToolFinders with(Iterable<ToolFinder> more) {
    var finders = new ArrayList<>(this.list);
    for (var finder : more) finders.add(finder);
    return new ToolFinders(List.copyOf(finders));
  }

  public ToolFinders with(String description, Iterable<ToolProvider> providers) {
    var tools = new ArrayList<Tool>();
    for (var provider : providers) tools.add(new Tool(provider));
    if (tools.isEmpty()) return this;
    return with(new Tools(description, tools));
  }

  public ToolFinders with(String description, Tool... tools) {
    return with(new Tools(description, List.of(tools)));
  }
}
