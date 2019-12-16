/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
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

package de.sormuras.bach.task;

import de.sormuras.bach.Log;
import de.sormuras.bach.project.Library;
import de.sormuras.bach.project.Template;
import de.sormuras.bach.project.Template.Placeholder;
import de.sormuras.bach.util.Maven;
import de.sormuras.bach.util.Uris;
import java.lang.module.ModuleDescriptor.Version;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

class Resolver {

  private final Log log;
  private final Library library;
  private final Uris uris;
  private final AtomicReference<Maven.Central> central = new AtomicReference<>(null);

  Resolver(Log log, Library library) {
    this.log = log;
    this.library = library;
    this.uris = Uris.of(log);
  }

  public void resolveRequires(Path lib) throws Exception {
    for (var required : library.requires()) {
      resolve(lib, required.name(), required.version());
    }
  }

  public void resolveModules(Path lib, Map<String, Set<Version>> modules) throws Exception {
    for (var entry : modules.entrySet()) {
      var module = entry.getKey();
      var version = singleton(entry.getValue()).orElse(null);
      resolve(lib, module, version);
    }
  }

  private void resolve(Path lib, String module, Version versionOrNull) throws Exception {
    var link = lookup(module, versionOrNull);
    var uri = link.reference();
    var jar = lib.resolve(module + '-' + link.version() + ".jar");
    uris.copy(URI.create(uri), jar, StandardCopyOption.COPY_ATTRIBUTES);
  }

  Library.Link lookup(String module) {
    var link = library.links().get(module);
    if (link != null) return link;
    return central().link(module); // create Maven Central link on-the-fly
  }

  Library.Link lookup(String module, Version versionOrNull) {
    var lookup = lookup(module);
    return replace(lookup, versionOrNull != null ? versionOrNull : lookup.version());
  }

  private Maven.Central central() {
    var current = central.get();
    if (current != null) return current;
    try {
      log.info("Create Maven Central link factory");
      var newCentral = new Maven.Central(uris);
      return central.compareAndSet(null, newCentral) ? newCentral : central.get();
    } catch (Exception e) {
      throw new AssertionError("Create Central failed", e);
    }
  }

  static Library.Link replace(Library.Link link, Version version) {
    var reference = link.reference();
    if (reference.indexOf('$') < 0) return link;
    var binding =
        Map.of(
            Placeholder.JAVAFX_PLATFORM, Placeholder.JAVAFX_PLATFORM.getDefault(),
            Placeholder.LWJGL_NATIVES, Placeholder.LWJGL_NATIVES.getDefault(),
            Placeholder.VERSION, version.toString());
    return new Library.Link(Template.replace(reference, binding), version);
  }

  private static <T> Optional<T> singleton(Collection<T> collection) {
    if (collection.isEmpty()) {
      return Optional.empty();
    }
    if (collection.size() != 1) {
      throw new IllegalStateException("Too many elements: " + collection);
    }
    return Optional.of(collection.iterator().next());
  }
}
