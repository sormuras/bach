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
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;

/** Resolve missing external modules. */
public /*static*/ class ModuleResolver {
  private final Path lib;
  private final Set<String> declared;
  private final BiConsumer<Set<String>, Path> downloader;
  private final Set<String> system;

  public ModuleResolver(Path lib, Set<String> declared, BiConsumer<Set<String>, Path> downloader) {
    this.lib = Objects.requireNonNull(lib, "lib");
    this.declared = new TreeSet<>(declared);
    this.downloader = Objects.requireNonNull(downloader, "downloader");
    this.system = declared(ModuleFinder.ofSystem());
  }

  public void resolve(Set<String> required) throws Exception {
    resolveModules(required);
    resolveLibraryModules();
  }

  public void resolveModules(Set<String> required) throws Exception {
    var missing = missing(required);
    if (missing.isEmpty()) return;
    Files.createDirectories(lib);
    downloader.accept(missing, lib);
    var unresolved = missing(required);
    if (unresolved.isEmpty()) return;
    throw new IllegalStateException("Unresolved modules: " + unresolved);
  }

  public void resolveLibraryModules() throws Exception {
    do {
      var missing = missing(required(ModuleFinder.of(lib)));
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
    var library = declared(ModuleFinder.of(lib));
    missing.removeAll(library);
    return missing;
  }

  static Set<String> declared(ModuleFinder finder) {
    return Modules.declared(finder.findAll().stream().map(ModuleReference::descriptor));
  }

  static Set<String> required(ModuleFinder finder) {
    return Modules.required(finder.findAll().stream().map(ModuleReference::descriptor));
  }
}
