package com.github.sormuras.bach.module;

import com.github.sormuras.bach.internal.Modules;
import com.github.sormuras.bach.internal.Paths;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * A module declaration finder backed by {@code module-info.java} compilation units.
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jls/se9/html/jls-7.html#jls-7.7">Module
 *     Declarations</a>
 */
public final class ModuleInfoFinder implements ModuleFinder {

  /**
   * @param directory the directory to walk
   * @param globs the module source paths to apply
   * @return a module declaration finder
   */
  public static ModuleInfoFinder of(Path directory, List<String> globs) {
    var references = new TreeMap<String, ModuleReference>();
    for (var glob : globs)
      Paths.find(
          directory,
          glob + "/module-info.java",
          info -> {
            var ref = ModuleInfoReference.of(info);
            references.put(ref.descriptor().name(), ref);
          });
    return new ModuleInfoFinder(references);
  }

  private final Map<String, ModuleReference> references;
  private final Set<ModuleReference> values;

  ModuleInfoFinder(Map<String, ModuleReference> references) {
    this.references = references;
    this.values = Set.copyOf(references.values());
  }

  @Override
  public Optional<ModuleReference> find(String name) {
    return Optional.ofNullable(references.get(name));
  }

  @Override
  public Set<ModuleReference> findAll() {
    return values;
  }

  /** @return a set of module names */
  public Set<String> declared() {
    return Modules.declared(this);
  }

  /** @return a set of module names */
  public Set<String> required() {
    return Modules.required(this);
  }
}
