package de.sormuras.bach;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Static helpers handling modules. */
public class Modules {

  private static final Pattern MODULE_NAME_PATTERN = Pattern.compile("(?:module)\\s+(.+)\\s*\\{.*");
  private static final Pattern MODULE_REQUIRES_PATTERN =
      Pattern.compile(
          "(?:requires)" // key word
              + "(?:\\s+[\\w.]+)?" // optional modifiers
              + "\\s+([\\w.]+)" // module name
              + "(?:\\s*/\\*\\s*([\\w.\\-+]+)\\s*\\*/\\s*)?" // optional version
              + ";.*"); // end marker

  /** Enumerate all system module names. */
  static Set<String> findSystemModuleNames() {
    return ModuleFinder.ofSystem().findAll().stream()
        .map(reference -> reference.descriptor().name())
        .collect(Collectors.toSet());
  }

  /** Calculate external module names. */
  static Set<String> findExternalModuleNames(Collection<ModuleDescriptor> descriptors) {
    var declaredModules = new TreeSet<String>();
    var requiredModules = new TreeSet<String>();
    for (var descriptor : descriptors) {
      declaredModules.add(descriptor.name());
      descriptor.requires().stream().map(Requires::name).forEach(requiredModules::add);
    }
    var externalModules = new TreeSet<>(requiredModules);
    externalModules.removeAll(declaredModules);
    externalModules.removeAll(findSystemModuleNames()); // "java.base", "java.logging", ...
    return externalModules;
  }

  /** Simplistic module declaration parser. */
  static ModuleDescriptor parseDeclaration(Path path) {
    if (!Util.isModuleInfo(path)) {
      throw new IllegalArgumentException("Expected module-info.java path, but got: " + path);
    }
    try {
      return parseDeclaration(Files.readString(path));
    } catch (IOException e) {
      throw new UncheckedIOException("Reading module declaration failed: " + path, e);
    }
  }

  /** Simplistic module declaration parser. */
  static ModuleDescriptor parseDeclaration(String source) {
    var nameMatcher = MODULE_NAME_PATTERN.matcher(source);
    if (!nameMatcher.find()) {
      throw new IllegalArgumentException("Expected Java module source unit, but got: " + source);
    }
    var name = nameMatcher.group(1).trim();
    var builder = ModuleDescriptor.newModule(name);
    var requiresMatcher = MODULE_REQUIRES_PATTERN.matcher(source);
    while (requiresMatcher.find()) {
      var requiredName = requiresMatcher.group(1);
      Optional.ofNullable(requiresMatcher.group(2))
          .ifPresentOrElse(
              version ->
                  builder.requires(Set.of(), requiredName, ModuleDescriptor.Version.parse(version)),
              () -> builder.requires(requiredName));
    }
    return builder.build();
  }
}
