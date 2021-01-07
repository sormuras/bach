package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.ComposedModuleLookup;
import com.github.sormuras.bach.internal.EmptyModuleLookup;
import com.github.sormuras.bach.internal.ExternalModuleLookup;
import com.github.sormuras.bach.internal.Maven;
import java.util.List;
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