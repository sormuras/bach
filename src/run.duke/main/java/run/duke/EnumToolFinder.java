package run.duke;

import java.util.List;
import java.util.Optional;

@FunctionalInterface
public interface EnumToolFinder<E extends Enum<E> & ToolInfo> extends ToolFinder {
  List<E> constants();

  @Override
  default List<String> identifiers() {
    return constants().stream().map(ToolInfo::identifier).toList();
  }

  @Override
  default Optional<Tool> find(String string, ToolRunner runner) {
    for (var info : constants()) if (info.test(string)) return Optional.of(info.tool(runner));
    return Optional.empty();
  }
}
