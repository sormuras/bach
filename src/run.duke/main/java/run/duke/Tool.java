package run.duke;

import java.util.function.Predicate;
import java.util.spi.ToolProvider;

/**
 * A named descriptor of a tool backed by a runnable tool provider.
 *
 * @param namespace the namespace of this tool descriptor, potentially empty, but never {@code null}
 * @param name the short name of this tool descriptor, never empty nor {@code null}
 * @param provider the backing tool provider instance
 */
public record Tool(String namespace, String name, ToolProvider provider)
    implements Predicate<String> {

  public static String namespace(Class<?> type) {
    var module = type.getModule();
    return module.isNamed() ? module.getName() : type.getPackageName();
  }

  /** Validate components. */
  public Tool {
    if (namespace == null) throw new IllegalArgumentException("Namespace must not be null");
    if (namespace.isBlank() && !namespace.isEmpty()) throw new IllegalArgumentException(namespace);
    if (namespace.startsWith("/")) throw new IllegalArgumentException(namespace);
    if (namespace.endsWith("/")) throw new IllegalArgumentException(namespace);
    if (name == null) throw new IllegalArgumentException("Tool name must not be null");
    if (name.isBlank()) throw new IllegalArgumentException("Tool name must not be blank");
    if (name.startsWith("/")) throw new IllegalArgumentException(name);
    if (name.endsWith("/")) throw new IllegalArgumentException(name);
    if (provider == null) throw new IllegalArgumentException("Tool provider must not be null");
  }

  /**
   * Initialize a tool instance from the given tool provider.
   *
   * @param provider the backing tool provider instance
   */
  public Tool(ToolProvider provider) {
    this(namespace(provider.getClass()), provider.name(), provider);
  }

  /**
   * {@return {@code true} for a probe that matches this tool's names, else {@code false}}
   *
   * @param probe the string to test against this tool's names
   */
  public boolean test(String probe) {
    return probe.equals(name) || probe.equals(identifier());
  }

  /** {@return a string composed of the namespace and the name joined by a slash {@code /}} */
  public String identifier() {
    return namespace.isEmpty() ? name : namespace + '/' + name;
  }
}
