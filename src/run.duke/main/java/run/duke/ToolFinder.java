package run.duke;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.spi.ToolProvider;
import run.duke.internal.CollectionToolFinder;
import run.duke.internal.PreparedToolFinder;

@FunctionalInterface
public interface ToolFinder {
  /** {@return a description of the finder, or an empty {@code String} if none is available} */
  default String description() {
    return getClass().getSimpleName();
  }

  /**
   * Find a tool by its identifier or its nickname.
   *
   * @param string the identifier or the short variant of the tool to look for
   * @param runner the tool runner context; sometimes required to create a tool instance
   * @return a tool instance wrapped in an optional, or an empty optional wrapper
   */
  Optional<Tool> find(String string, ToolRunner runner);

  /** {@return a possibly empty list of tool identifying-strings} */
  default List<String> identifiers() {
    return List.of();
  }

  static ToolFinder ofTools(String description, Tool... tools) {
    return ToolFinder.ofTools(description, List.of(tools));
  }

  static ToolFinder ofTools(String description, Collection<Tool> tools) {
    return new CollectionToolFinder(description, List.copyOf(tools));
  }

  static ToolFinder ofToolCalls(String description, Collection<ToolCalls> calls) {
    return new PreparedToolFinder(description, List.copyOf(calls));
  }

  static ToolFinder ofToolProviders(String description, Iterable<ToolProvider> providers) {
    var tools = new ArrayList<Tool>();
    for (var provider : providers) tools.add(new Tool(provider));
    return ToolFinder.ofTools(description, tools);
  }

  @FunctionalInterface
  interface EnumToolFinder<E extends Enum<E> & ToolInfo, R extends ToolRunner> extends ToolFinder {
    List<E> constants();

    @Override
    default List<String> identifiers() {
      return constants().stream().map(ToolInfo::identifier).toList();
    }

    @Override
    default Optional<Tool> find(String string, ToolRunner runner) {
      @SuppressWarnings("unchecked")
      var casted = (R) runner;
      for (var info : constants()) if (info.test(string)) return Optional.of(tool(info, casted));
      return Optional.empty();
    }

    default Tool tool(E info, R runner) {
      return info.tool(runner);
    }
  }
}
