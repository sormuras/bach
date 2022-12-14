package run.duke;

import java.util.function.Predicate;
import java.util.spi.ToolProvider;

public sealed interface Tool extends Comparable<Tool>, Predicate<String> {
  String identifier();

  default String namespace() {
    var identifier = identifier();
    var separator = identifier.lastIndexOf('/');
    return separator == -1 ? "" : identifier.substring(0, separator);
  }

  default String nickname() {
    var identifier = identifier();
    return identifier.substring(identifier.lastIndexOf('/') + 1);
  }

  @Override
  default int compareTo(Tool other) {
    return identifier().compareTo(other.identifier());
  }

  @Override
  default boolean test(String tool) {
    var identifier = identifier();
    return identifier.equals(tool) || identifier.endsWith('/' + tool);
  }

  static String namespace(Class<?> type) {
    var module = type.getModule();
    return module.isNamed() ? module.getName() : type.getPackageName();
  }

  static Tool of(ToolProvider provider) {
    var identifier = namespace(provider.getClass()) + '/' + provider.name();
    return Tool.of(identifier, provider);
  }

  static Tool of(ToolOperator operator) {
    var identifier = namespace(operator.getClass()) + '/' + operator.name();
    return Tool.of(identifier, operator);
  }

  static Tool of(String identifier, ToolProvider provider) {
    return new OfProvider(identifier, provider);
  }

  static Tool of(String identifier, ToolOperator operator) {
    return new OfOperator(identifier, operator);
  }

  static ToolProvider provider(Tool tool, ToolRunner runner) {
    if (tool instanceof Tool.OfProvider of) return of.provider();
    if (tool instanceof Tool.OfOperator of) return of.operator().provider(runner);
    throw new Error("Unsupported tool of " + tool.getClass());
  }

  record OfProvider(String identifier, ToolProvider provider) implements Tool {}

  record OfOperator(String identifier, ToolOperator operator) implements Tool {}
}
