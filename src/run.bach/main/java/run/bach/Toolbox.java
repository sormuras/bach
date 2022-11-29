package run.bach;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.spi.ToolProvider;
import run.bach.internal.InstanceToolFinder;

public record Toolbox(List<ToolFinder> finders) implements ToolFinder {
  public static final Toolbox EMPTY = new Toolbox();

  public Toolbox() {
    this(List.of());
  }

  @Override
  public String description() {
    return "Toolbox (%d)".formatted(finders.size());
  }

  public int size() {
    int size = 0;
    for (var finder : finders) size += finder.identifiers().size();
    return size;
  }

  public Optional<Tool> find(String name, ToolRunner runner) {
    for (var finder : finders) {
      var tool = finder.find(name, runner);
      if (tool.isPresent()) return tool;
    }
    return Optional.empty();
  }

  public Toolbox with(ToolFinder finder, ToolFinder... more) {
    var finders = new ArrayList<>(this.finders);
    finders.add(finder);
    if (more.length > 0) finders.addAll(List.of(more));
    return new Toolbox(List.copyOf(finders));
  }

  public Toolbox with(Iterable<ToolFinder> more) {
    var finders = new ArrayList<>(this.finders);
    for (var finder : more) finders.add(finder);
    return new Toolbox(List.copyOf(finders));
  }

  public Toolbox with(String description, Iterable<ToolProvider> providers) {
    var tools = new ArrayList<Tool>();
    for (var provider : providers) tools.add(new Tool(provider));
    if (tools.isEmpty()) return this;
    return with(new InstanceToolFinder(description, tools));
  }

  public Toolbox with(String description, Tool... tools) {
    return with(new InstanceToolFinder(description, tools));
  }
}
