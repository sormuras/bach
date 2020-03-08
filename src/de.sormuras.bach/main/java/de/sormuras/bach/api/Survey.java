/*
 * Bach - Java Shell Builder
 * Copyright (C) 2020 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.sormuras.bach.api;

import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

/** Declared and required modules and optional versions holder. */
public /*static*/ final class Survey {

  /** Create modular survey by scanning the locatable modules of the given module finder. */
  public static Survey of(ModuleFinder finder) {
    return of(finder.findAll().stream().map(ModuleReference::descriptor));
  }

  /** Create modular survey by scanning the given stream of module descriptors. */
  public static Survey of(Stream<ModuleDescriptor> descriptors) {
    var declaredModules = new TreeSet<String>();
    var requiredModules = new TreeMap<String, Version>();
    descriptors
        .peek(descriptor -> declaredModules.add(descriptor.name()))
        .map(ModuleDescriptor::requires)
        .flatMap(Set::stream)
        .filter(requires -> !requires.modifiers().contains(Requires.Modifier.STATIC))
        .filter(requires -> !requires.modifiers().contains(Requires.Modifier.MANDATED))
        .forEach(
            requires -> {
              var module = requires.name();
              var version = requires.compiledVersion().orElse(null);
              var previous = requiredModules.putIfAbsent(module, version);
              if (previous == null || version == null || previous.equals(version)) return;
              throw new IllegalArgumentException(previous + " != " + version);
            });
    return new Survey(declaredModules, requiredModules);
  }

  final Set<String> declaredModules;
  final Map<String, Version> requiredModules;

  Survey(Set<String> declaredModules, Map<String, Version> requiredModules) {
    this.declaredModules = declaredModules;
    this.requiredModules = requiredModules;
  }

  public Set<String> declaredModules() {
    return declaredModules;
  }

  public Map<String, Version> requiredModules() {
    return requiredModules;
  }

  public Set<String> requiredModuleNames() {
    return requiredModules.keySet();
  }

  public Optional<Version> requiredVersion(String requiredModule) {
    var unmapped = !requiredModules.containsKey(requiredModule);
    if (unmapped) throw new FindException(requiredModule);
    return Optional.ofNullable(requiredModules.get(requiredModule));
  }
}
