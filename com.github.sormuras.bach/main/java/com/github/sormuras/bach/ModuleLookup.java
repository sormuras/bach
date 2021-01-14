package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.ComposedModuleLookup;
import com.github.sormuras.bach.internal.DynamicModuleLookup;
import com.github.sormuras.bach.internal.EmptyModuleLookup;
import com.github.sormuras.bach.internal.ExternalModuleLookup;
import com.github.sormuras.bach.internal.MappedModuleLookup;
import com.github.sormuras.bach.internal.Maven;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** A function that tries to link a module name to a specific uniform resource identifier. */
@FunctionalInterface
public interface ModuleLookup {

  /**
   * Returns an optional uniform resource identifier for the given module name.
   *
   * @param module the name of the module to lookup
   * @return a string-representation of a uniform resource identifier wrapped in an optional object
   */
  Optional<String> lookupModule(String module);

  /**
   * Returns a module lookup that is composed from a sequence of zero or more module lookup objects.
   *
   * @param lookups the array of module lookups
   * @return a module lookup that composes a sequence of module lookup objects
   */
  static ModuleLookup compose(ModuleLookup... lookups) {
    if (lookups.length == 0) return ofEmpty();
    if (lookups.length == 1) return lookups[0];
    return new ComposedModuleLookup(List.of(lookups));
  }

  /** @return an always empty-returning module lookup */
  static ModuleLookup ofEmpty() {
    return EmptyModuleLookup.SINGLETON;
  }

  /** {@return a module lookup that is created from or backed by a copy of the given map} */
  static ModuleLookup of(Map<String, String> map) {
    if (map.isEmpty()) return ofEmpty();
    if (map.size() == 1)
      return map.entrySet().stream()
          .findFirst()
          .map(first -> of(first.getKey(), first.getValue()))
          .orElseThrow();
    return new MappedModuleLookup(Map.copyOf(map));
  }

  /** {@return an external module lookup that links the given module name to the given uri} */
  static ModuleLookup of(String module, String uri) {
    return linkExternalModule(module).toUri(uri);
  }

  /** {@return a module lookup that is backed by providers found among the external modules} */
  static ModuleLookup ofProvidersFoundInExternalModules() {
    return new DynamicModuleLookup(Bach.EXTERNALS);
  }

  /** @return an external module lookup builder */
  static Builder linkExternalModule(String module) {
    return new Builder(module);
  }

  /** A builder for building module lookup objects linking an external module. */
  record Builder(String module) {

    public ModuleLookup toMaven(String repository, String group, String artifact, String version) {
      return toUri(Maven.Joiner.of(group, artifact, version).repository(repository).toString());
    }

    public ModuleLookup toMavenCentral(String group, String artifact, String version) {
      return toUri(Maven.central(group, artifact, version));
    }

    public ModuleLookup toUri(String uri) {
      return new ExternalModuleLookup(module, uri);
    }
  }
}
