package com.github.sormuras.bach.project;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Builder;
import java.lang.module.ModuleDescriptor.Modifier;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @see <a href="https://docs.oracle.com/javase/specs/jls/se9/html/jls-7.html#jls-7.7">Module
 *     Declarations</a>
 */
public final class ModuleInfoReference extends ModuleReference {

  /** Match {@code `module Identifier {. Identifier}`} snippets. */
  private static final Pattern NAME =
      Pattern.compile(
          "module" // key word
              + "\\s+([\\w.]+)" // module name
              + "(?:\\s*/\\*.*\\*/\\s*)?" // optional multi-line comment
              + "\\s*\\{"); // end marker

  /** Match {@code `requires {RequiresModifier} ModuleName ;`} snippets. */
  private static final Pattern REQUIRES =
      Pattern.compile(
          "requires" // key word
              + "(?:\\s+[\\w.]+)?" // optional modifiers
              + "\\s+([\\w.]+)" // module name
              + "(?:\\s*/\\*\\s*([\\w.\\-+]+)\\s*\\*/\\s*)?" // optional '/*' version '*/'
              + "\\s*;"); // end marker

  /**
   * @param info the path to the {@code module-info.java} file to parse
   * @return a module reference
   */
  public static ModuleInfoReference of(Path info) {
    return new ModuleInfoReference(info);
  }

  /** Parse module definition from the given file. */
  public static ModuleDescriptor parse(Path info) {
    try {
      var module = parse(Files.readString(info));
      var temporary = module.build();
      findMainClass(info, temporary.name()).ifPresent(module::mainClass);
      return module.build();
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  /** Parse module definition from the given source code. */
  public static Builder parse(String text) {
    var source =
        text.lines()
            .filter(line -> !line.trim().startsWith("//"))
            .collect(Collectors.joining("\n"));
    // `module Identifier {. Identifier}`
    var nameMatcher = NAME.matcher(source);
    if (!nameMatcher.find())
      throw new IllegalArgumentException("Expected Java module source unit, but got: " + source);
    var name = nameMatcher.group(1).trim();
    var builder = ModuleDescriptor.newModule(name, Set.of(Modifier.SYNTHETIC));
    // "requires module /*version*/;"
    var requiresMatcher = REQUIRES.matcher(source);
    while (requiresMatcher.find()) {
      var requiredName = requiresMatcher.group(1);
      Optional.ofNullable(requiresMatcher.group(2))
          .ifPresentOrElse(
              version -> builder.requires(Set.of(), requiredName, Version.parse(version)),
              () -> builder.requires(requiredName));
    }
    return builder;
  }

  /** Return name of main class of the specified module. */
  public static Optional<String> findMainClass(Path info, String module) {
    var main = Path.of(module.replace('.', '/'), "Main.java");
    var exists = Files.isRegularFile(info.resolveSibling(main));
    return exists ? Optional.of(module + '.' + "Main") : Optional.empty();
  }

  private final Path info;

  private ModuleInfoReference(Path info) {
    super(parse(info), info.toUri());
    this.info = info;
  }

  /** {@return the path to the {@code module-info.java} file} */
  public Path info() {
    return info;
  }

  /** {@return the name of the module} */
  public String name() {
    return descriptor().name();
  }

  @Override
  public ModuleReader open() {
    return new NullModuleReader();
  }

  @Override
  public String toString() {
    return "ModuleInfoReference[info=" + info + ']';
  }

  private static class NullModuleReader implements ModuleReader {
    private NullModuleReader() {}

    @Override
    public Optional<URI> find(String name) {
      return Optional.empty();
    }

    @Override
    public Stream<String> list() {
      return Stream.empty();
    }

    @Override
    public void close() {}
  }
}
