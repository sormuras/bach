package run.duke;

import java.util.function.Predicate;

@FunctionalInterface
public interface ToolInfo extends Predicate<String> {
  /** {@return a string composed of the namespace and the nickname joined by a slash {@code /}} */
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

  /**
   * {@return {@code true} for a string that matches this tool's identifier, else {@code false}}
   *
   * @param string the string to test against this tool's identifier
   */
  @Override
  default boolean test(String string) {
    var identifier = identifier();
    return identifier.equals(string) || identifier.endsWith('/' + string);
  }

  default Tool tool(ToolRunner runner) {
    throw new UnsupportedOperationException();
  }
}
