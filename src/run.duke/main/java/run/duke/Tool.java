package run.duke;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.spi.ToolProvider;

/**
 * An identifiable abstraction for tool providers.
 *
 * <p>An identifier is a string composed of a possibly empty namespace component and an always
 * non-empty nickname component. The last {@code /} character in an identifier separates the
 * namespace from the nickname.
 *
 * <table>
 *   <tr>
 *     <th> Identifier </th>
 *     <th> Namespace </th>
 *     <th> Nickname </th>
 *   </tr>
 *   </th>
 *   <tr>
 *     <td> {@code "jdk.compiler/javac"} </td>
 *     <td> {@code "jdk.compiler"} </td>
 *     <td> {@code "javac"} </td>
 *   </tr>
 *   <tr>
 *     <td> {@code "deep/space/nine"} </td>
 *     <td> {@code "deep/space"} </td>
 *     <td> {@code "nine"} </td>
 *   </tr>
 *   <tr>
 *     <td> {@code "/tool"} </td>
 *     <td> {@code ""} </td>
 *     <td> {@code "tool"} </td>
 *   </tr>
 * </table>
 */
public sealed interface Tool extends Comparable<Tool>, Predicate<String> {
  /** {@return the string identifying this tool instance} */
  String identifier();

  /** {@return the namespace of this tool instance, possibly an empty string} */
  default String namespace() {
    return namespace(identifier());
  }

  /** {@return the non-empty nickname of this tool instance} */
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
    return matches(identifier(), tool);
  }

  static void checkIdentifier(String identifier) throws RuntimeException {
    Objects.requireNonNull(identifier, "identifier must not be null");
    if (identifier.isEmpty()) throw new IllegalArgumentException("identifier must not be empty");
    if (identifier.isBlank()) throw new IllegalArgumentException("identifier must not be blank");
    Objects.checkIndex(identifier.lastIndexOf('/'), identifier.length() - 1);
  }

  static boolean matches(String identifier, String tool) {
    return identifier.equals(tool) || identifier.endsWith('/' + tool);
  }

  /** {@return a namespace string derived from the domain of the type argument} */
  static String namespace(Class<?> type) {
    var module = type.getModule();
    return module.isNamed() ? module.getName() : type.getPackageName();
  }

  static String namespace(String tool) {
    var separator = tool.lastIndexOf('/');
    return separator == -1 ? "" : tool.substring(0, separator);
  }

  static String nickname(String tool) {
    return tool.substring(tool.lastIndexOf('/') + 1);
  }

  /** {@return a tool for the given provider instance deriving the namespace from it} */
  static Tool of(ToolProvider provider) {
    var identifier = namespace(provider.getClass()) + '/' + provider.name();
    return Tool.of(identifier, provider);
  }

  /** {@return a tool for the given operator instance deriving the namespace from it} */
  static Tool of(ToolOperator operator) {
    var identifier = namespace(operator.getClass()) + '/' + operator.name();
    return Tool.of(identifier, operator);
  }

  /** {@return a tool for the given identifier and the given provider instance} */
  static Tool of(String identifier, ToolProvider provider) {
    return new OfProvider(identifier, provider);
  }

  /** {@return a tool for the given identifier and the given operator instance} */
  static Tool of(String identifier, ToolOperator operator) {
    return new OfOperator(identifier, operator);
  }

  /** Tool of {@link ToolProvider}. */
  record OfProvider(String identifier, ToolProvider provider) implements Tool {
    public OfProvider {
      checkIdentifier(identifier);
      Objects.requireNonNull(provider, "provider must not be null");
    }
  }

  /** Tool of {@link ToolOperator}. */
  record OfOperator(String identifier, ToolOperator operator) implements Tool {
    public OfOperator {
      checkIdentifier(identifier);
      Objects.requireNonNull(operator, "operator must not be null");
    }
  }
}
