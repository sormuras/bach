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

package de.sormuras.bach.internal;

import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

/** Resolve required modules, unless a module can be found. */
public /*static*/ class ModulesResolver {

  private final Path[] paths;
  private final Set<String> declared;
  private final Consumer<Set<String>> transporter;
  private final Set<String> system;

  public ModulesResolver(List<Path> paths, Set<String> declared, Consumer<Set<String>> transporter) {
    this.paths = paths.toArray(Path[]::new);
    this.declared = new TreeSet<>(declared);
    this.transporter = transporter;
    this.system = Modules.declared(ModuleFinder.ofSystem());
  }

  public void resolve(Set<String> required) {
    resolveModules(required);
    resolveLibraryModules();
  }

  public void resolveModules(Set<String> required) {
    var missing = missing(required);
    if (missing.isEmpty()) return;
    transporter.accept(missing);
    var unresolved = missing(required);
    if (unresolved.isEmpty()) return;
    throw new IllegalStateException("Unresolved modules: " + unresolved);
  }

  public void resolveLibraryModules() {
    do {
      var missing = missing(Modules.required(ModuleFinder.of(paths)));
      if (missing.isEmpty()) return;
      resolveModules(missing);
    } while (true);
  }

  Set<String> missing(Set<String> required) {
    var missing = new TreeSet<>(required);
    missing.removeAll(declared);
    if (required.isEmpty()) return Set.of();
    missing.removeAll(system);
    if (required.isEmpty()) return Set.of();
    var library = Modules.declared(ModuleFinder.of(paths));
    missing.removeAll(library);
    return missing;
  }
}
