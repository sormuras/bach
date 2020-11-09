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
   * @param moduleSourcePaths the module source paths to apply
   * @return a module declaration finder
   */
  public static ModuleInfoFinder of(Path directory, List<String> moduleSourcePaths) {
    var references = new TreeMap<String, ModuleReference>();
    for (var segment : moduleSourcePaths) {
      var glob = new StringBuilder();
      glob.append(segment);
      if (segment.indexOf('*') < 0) glob.append("/*");
      if (!glob.toString().endsWith("/")) glob.append("/");
      glob.append("module-info.java");
      Paths.find(
          directory,
          glob.toString(),
          info -> {
            var ref = ModuleInfoReference.of(info);
            references.put(ref.descriptor().name(), ref);
          });
    }
    return new ModuleInfoFinder(moduleSourcePaths, references);
  }

  private final List<String> moduleSourcePaths;
  private final Map<String, ModuleReference> references;
  private final Set<ModuleReference> values;

  ModuleInfoFinder(List<String> moduleSourcePaths, Map<String, ModuleReference> references) {
    this.moduleSourcePaths = moduleSourcePaths;
    this.references = references;
    this.values = Set.copyOf(references.values());
  }

  /** @return the configured module source path patterns */
  public List<String> moduleSourcePaths() {
    return moduleSourcePaths;
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
