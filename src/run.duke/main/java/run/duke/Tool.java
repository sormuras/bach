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
    return new OfProvider(identifier, provider);
  }

  static Tool of(ToolOperator operator) {
    var identifier = namespace(operator.getClass()) + '/' + operator.name();
    return new OfOperator(identifier, operator);
  }

  record OfProvider(String identifier, ToolProvider provider) implements Tool {}

  record OfOperator(String identifier, ToolOperator operator) implements Tool {}
}
