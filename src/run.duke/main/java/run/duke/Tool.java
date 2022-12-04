package run.duke;

import java.util.spi.ToolProvider;

/**
 * A named descriptor of a tool backed by a runnable tool provider.
 *
 * @param provider the backing tool provider instance
 */
public record Tool(String identifier, ToolProvider provider) implements ToolInfo {
  public static String identifier(Class<?> type, String nickname) {
    return identifier(namespace(type), nickname);
  }

  public static String identifier(String namespace, String nickname) {
    if (namespace == null) throw new IllegalArgumentException("namespace must not be null");
    if (namespace.length() > 0) {
      if (namespace.isBlank()) throw new IllegalArgumentException("namespace must not be blank");
      if (namespace.startsWith("/")) throw new IllegalArgumentException(namespace);
      if (namespace.endsWith("/")) throw new IllegalArgumentException(namespace);
    }
    if (nickname == null) throw new IllegalArgumentException("nickname must not be null");
    if (nickname.isBlank()) throw new IllegalArgumentException("nickname must not be blank");
    if (nickname.startsWith("/")) throw new IllegalArgumentException(nickname);
    if (nickname.endsWith("/")) throw new IllegalArgumentException(nickname);
    return namespace.isEmpty() ? nickname : namespace + '/' + nickname;
  }

  public static String namespace(Class<?> type) {
    var module = type.getModule();
    return module.isNamed() ? module.getName() : type.getPackageName();
  }

  /** Validate components. */
  public Tool {
    if (identifier == null) throw new IllegalArgumentException("identifier must not be null");
    if (identifier.isBlank()) throw new IllegalArgumentException("identifier must not be blank");
    if (identifier.startsWith("/")) throw new IllegalArgumentException(identifier);
    if (identifier.endsWith("/")) throw new IllegalArgumentException(identifier);
    if (provider == null) throw new IllegalArgumentException("provider must not be null");
  }

  public Tool(String namespace, String nickname, ToolProvider provider) {
    this(identifier(namespace, nickname), provider);
  }

  /**
   * Initialize a tool instance from the given tool provider.
   *
   * @param provider the backing tool provider instance
   */
  public Tool(ToolProvider provider) {
    this(identifier(provider.getClass(), provider.name()), provider);
  }
}
