package run.duke;

import java.util.List;
import java.util.Optional;

@FunctionalInterface
public interface EnumToolFinder<E extends Enum<E> & ToolInfo, R extends ToolRunner> extends ToolFinder {
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
