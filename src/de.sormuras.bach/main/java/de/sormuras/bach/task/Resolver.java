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
import de.sormuras.bach.util.Modules;
import de.sormuras.bach.util.Uris;
import java.lang.module.ModuleDescriptor.Version;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

class Resolver {
  private final Log log;
  private final Library library;
  private final Uris uris;

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

  Library.Link lookup(String module, Version versionOrNull) {
    log.debug("Resolving module %s [%s]", module, versionOrNull);
    var link = library.links().get(module);
    if (link == null) {
      // TODO Fall back to https://github.com/sormuras/modules database
      throw new Modules.UnmappedModuleException(module);
    }
    return replace(link, versionOrNull != null ? versionOrNull : link.version());
  }

  Library.Link replace(Library.Link link, Version version) {
    var os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
    var javafxPlatform = os.contains("win") ? "win" : os.contains("mac") ? "mac" : "linux";
    var replaced =
        link.reference()
            .replace(Library.Link.VERSION, version.toString())
            .replace(Library.Link.JAVAFX_PLATFORM, javafxPlatform);
    return new Library.Link(replaced, version);
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
