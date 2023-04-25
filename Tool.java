package run.bach;

import java.nio.file.Path;
import java.util.Optional;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

/**
 * Links tool-identifying names to an instance of the tool provider interface.
 *
 * @param identifier the nominal representation of the tool
 * @param provider the linked tool provider instance
 */
public record Tool(Identifier identifier, ToolProvider provider) {
  /**
   * {@return an instance of a tool specified its name}
   *
   * @param name the name of the tool to lookup
   * @throws NoSuchToolException when no tool could be found for the given name
   */
  public static Tool of(String name) {
    // Try with loading tool provider implementations using the system class loader first.
    var provider = ToolProvider.findFirst(name);
    if (provider.isPresent()) {
      return Tool.of(provider.get());
    }
    // Find executable tool program in JDK's binary directory.
    var executable = ToolProgram.findJavaDevelopmentKitTool(name);
    if (executable.isPresent()) {
      var version = String.valueOf(Runtime.version().feature());
      // var identifier = Identifier.of("jdk.home/bin", name, version);
      var identifier = Identifier.parse("jdk.home/bin/" + name + '@' + version);
      return new Tool(identifier, executable.get());
    }
    // Still here? Not so good...
    throw new NoSuchToolException(name);
  }

  /**
   * {@return an instance of tool for the given tool provider instance}
   *
   * @param provider the instance to use
   */
  public static Tool of(ToolProvider provider) {
    return new Tool(Identifier.of(provider), provider);
  }

  /**
   * {@return an instance of the tool specified by the name}
   *
   * @param provider the name of the tool to lookup
   */
  public static Tool of(String namespace, String name, String version, ToolProvider provider) {
    return new Tool(Identifier.of(namespace, name, version), provider);
  }

  /**
   * Describes a tool by its name in a possibly empty namespace and an optional version string.
   *
   * <p>Example: {@code "jdk.compiler/javac@99"}
   *
   * @param namespace a path-like group identifier of this tool or an empty string
   * @param name the name of this tool
   * @param version an optional version of this tool
   * @see ToolProvider#name()
   */
  public record Identifier(String namespace, String name, Optional<String> version) {
    public static Identifier of(String namespace, String name, String version) {
      return new Identifier(namespace, name, Optional.ofNullable(version));
    }

    public static Identifier of(ToolProvider provider) {
      var type = provider.getClass();
      var module = type.getModule();
      var namespace = module.isNamed() ? module.getName() : type.getPackageName();
      var name = provider.name();
      var version =
          module.isNamed()
              ? module.getDescriptor().version().map(Object::toString).orElse(null)
              : null;
      return Identifier.of(namespace, name, version);
    }

    public static Identifier parse(String id) { // ["namespace" "/"] "name" ["@" "version"]
      if (id == null) throw new NullPointerException("id must not be null");
      if (id.isBlank()) throw new IllegalArgumentException("id must not be blank");
      var path = Path.of(id).normalize();
      var elements = path.getNameCount();
      if (elements == 0) throw new IllegalArgumentException("only redundant elements in: " + id);
      var namespace = elements == 1 ? "" : path.getParent().toString().replace('\\', '/');
      var file = path.getFileName().toString();
      var separator = file.indexOf('@');
      var name = separator == -1 ? file : file.substring(0, separator);
      var version = separator == -1 ? null : file.substring(separator + 1);
      return Identifier.of(namespace, name, version);
    }

    public Identifier {
      if (Stream.of("/", "\\").anyMatch(namespace::startsWith))
        throw new IllegalArgumentException("Namespace must not start with / \\");
      if (Stream.of("/", "\\").anyMatch(namespace::endsWith))
        throw new IllegalArgumentException("Namespace must not end with / \\");
      if (Stream.of("/", "\\", "@").anyMatch(name::contains))
        throw new IllegalArgumentException("Name must not contain / \\ @");
    }

    public boolean matches(String string) {
      if (name.equals(string)) return true; // "javac"
      if (toNamespaceAndName().equals(string)) return true; // "jdk.compiler/javac"
      if (version.isPresent()) {
        if (toNameAndVersion().equals(string)) return true; // "javac@99"
        return toNamespaceAndNameAndVersion().equals(string); // "jdk.compiler/javac@99"
      }
      return false;
    }

    public String toNameAndVersion() {
      return version.map(version -> name + '@' + version).orElse(name);
    }

    public String toNamespaceAndName() {
      return namespace + '/' + name;
    }

    public String toNamespaceAndNameAndVersion() {
      return namespace + '/' + toNameAndVersion();
    }
  }
}
