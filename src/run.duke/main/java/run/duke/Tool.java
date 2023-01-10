package run.duke;

import java.util.Objects;
import java.util.Set;
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
 *
 * @param identifier the string identifying this tool instance
 * @param provider the underlying tool provider instance
 * @param tags a set of strings helping to categorize this tool instance
 */
public record Tool(String identifier, ToolProvider provider, Set<String> tags)
    implements Comparable<Tool> {
  /** {@return a tool for the given provider instance deriving the namespace from it} */
  public static Tool of(ToolProvider provider, String... tags) {
    var identifier = namespace(provider.getClass()) + '/' + provider.name();
    return Tool.of(identifier, provider, tags);
  }

  /** {@return a tool for the given identifier and the given provider instance} */
  public static Tool of(String identifier, ToolProvider provider, String... tags) {
    return new Tool(identifier, provider, Set.of(tags));
  }

  /**
   * {@return {@code true} if the given string matches the identifier}
   *
   * @param identifier the identifier of a tool instance
   * @param tool the string to check, typically also an identifier or a nickname
   */
  public static boolean matches(String identifier, String tool) {
    return identifier.equals(tool) || identifier.endsWith('/' + tool);
  }

  /** {@return a namespace string derived from the domain of the type argument} */
  public static String namespace(Class<?> type) {
    var module = type.getModule();
    return module.isNamed() ? module.getName() : type.getPackageName();
  }

  /** {@return a namespace string derived from the given tool, possibly an empty string} */
  public static String namespace(String tool) {
    var separator = tool.lastIndexOf('/');
    return separator == -1 ? "" : tool.substring(0, separator);
  }

  /** {@return a nickname string derived from the given tool} */
  public static String nickname(String tool) {
    return tool.substring(tool.lastIndexOf('/') + 1);
  }

  public Tool {
    Objects.requireNonNull(identifier, "identifier must not be null");
    if (identifier.isEmpty()) throw new IllegalArgumentException("identifier must not be empty");
    if (identifier.isBlank()) throw new IllegalArgumentException("identifier must not be blank");
    Objects.checkIndex(identifier.lastIndexOf('/'), identifier.length() - 1);
    Objects.requireNonNull(provider, "provider must not be null");
  }

  /** {@return the namespace of this tool instance, possibly an empty string} */
  public String namespace() {
    return namespace(identifier);
  }

  /** {@return the non-empty nickname of this tool instance} */
  public String nickname() {
    return nickname(identifier);
  }

  @Override
  public int compareTo(Tool other) {
    return identifier.compareTo(other.identifier);
  }

  /**
   * Evaluates the names of this tool on the given argument.
   *
   * @param identifierOrNickname the identifier or nickname to test
   * @return {@code true} if the input argument matches this tool's names, otherwise {@code false}
   */
  public boolean matches(String identifierOrNickname) {
    return matches(identifier, identifierOrNickname);
  }
}
