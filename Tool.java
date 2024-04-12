/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

/**
 * Links tool-identifying names to an instance of a tool provider interface implementation.
 *
 * @param identifier the nominal representation of this tool
 * @param provider the tool provider instance to link
 */
public record Tool(Identifier identifier, ToolProvider provider) {

  /**
   * {@return an instance of a tool specified its name}
   *
   * @param name the name of the tool to look up
   * @throws ToolNotFoundException when no tool could be found for the given name
   * @see ToolProvider#findFirst(String)
   * @see ToolProgram#findJavaDevelopmentKitTool(String, String...)
   */
  public static Tool of(String name) throws ToolNotFoundException {
    // Try with loading tool provider implementations using the system class loader first.
    var provider = ToolProvider.findFirst(name);
    if (provider.isPresent()) {
      return Tool.of(provider.get());
    }
    // Find executable tool program in JDK's binary directory.
    var program = ToolProgram.findJavaDevelopmentKitTool(name);
    if (program.isPresent()) {
      var version = String.valueOf(Runtime.version().feature());
      var identifier = Identifier.of("jdk.home/bin/" + name + '@' + version);
      return Tool.of(identifier, program.get());
    }
    // Try with treating the name argument as a URI.
    var installer = ToolInstaller.find(name);
    if (installer.isPresent()) {
      return Tool.of(installer.get(), ToolInstaller.Mode.INSTALL_IMMEDIATE);
    }
    // Still here? Not so good...
    throw new ToolNotFoundException("Tool not found for name: " + name);
  }

  /**
   * {@return an instance of tool linking the given tool provider instance}
   *
   * @param provider the tool provider instance to link and extract tool-identifiable names from
   * @see Identifier#of(ToolProvider)
   */
  public static Tool of(ToolProvider provider) {
    return new Tool(Identifier.of(provider), provider);
  }

  /**
   * {@return an instance of tool for the given identifier and provider}
   *
   * @param identifier the nominal representation of the tool
   * @param provider the tool provider instance to link
   */
  public static Tool of(Identifier identifier, ToolProvider provider) {
    return new Tool(identifier, provider);
  }

  /**
   * {@return an instance of tool for the given identifier and provider}
   *
   * @param id the nominal representation of the tool
   * @param provider the tool provider instance to link
   */
  public static Tool of(String id, ToolProvider provider) {
    return new Tool(Identifier.of(id), provider);
  }

  /**
   * {@return an instance of tool for the given identifier and tool provider supplier}
   *
   * @param id the nominal representation of the tool
   * @param supplier the supplier of the tool provider instance to wrap
   */
  public static Tool of(String id, Supplier<ToolProvider> supplier) {
    return Tool.of(Identifier.of(id), supplier);
  }

  /**
   * {@return an instance of tool for the given identifier and tool provider supplier}
   *
   * @param identifier the nominal representation of the tool
   * @param supplier the supplier of the tool provider instance to wrap
   */
  public static Tool of(Identifier identifier, Supplier<ToolProvider> supplier) {
    record Intermediary(String name, Supplier<ToolProvider> supplier) implements ToolProvider {
      @Override
      public int run(PrintWriter out, PrintWriter err, String... args) {
        return supplier.get().run(out, err, args);
      }
    }
    return new Tool(identifier, new Intermediary(identifier.name(), supplier));
  }

  public static Tool of(ToolInstaller installer) {
    return Tool.of(installer, ToolInstaller.Mode.DEFAULT);
  }

  public static Tool of(ToolInstaller installer, ToolInstaller.Mode mode) {
    return installer.install(mode);
  }

  public Tool {
    Objects.requireNonNull(identifier);
    Objects.requireNonNull(provider);
  }

  public void run(String... args) {
    ToolCall.of(this, args).run();
  }

  public void run(UnaryOperator<ToolCall> operator) {
    operator.apply(ToolCall.of(this)).run();
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

    public static Identifier of(ToolInstaller installer) {
      var type = installer.getClass();
      var module = type.getModule();
      var namespace = module.isNamed() ? module.getName() : type.getPackageName();
      var name = installer.name();
      var version = installer.version();
      return Identifier.of(namespace, name, version);
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

    public static Identifier of(String id) { // ["namespace" "/"] "name" ["@" "version"]
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
