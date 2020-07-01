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

package de.sormuras.bach.project;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** An external modules manager backed by the {@link Base#libraries() lib} directory. */
public final class Library {

  public static Library of() {
    return new Library(Set.of(), Map.of());
  }

  public Library with(String... moreRequires) {
    var requires = new TreeSet<>(requires());
    Collections.addAll(requires, moreRequires);
    return new Library(requires, links);
  }

  public Library with(Link... moreLinks) {
    var links = new TreeMap<>(links());
    for (var link : moreLinks) links.put(link.module(), link);
    return new Library(requires, links);
  }

  private final Set<String> requires;
  private final Map<String, Link> links;

  public Library(Set<String> requires, Map<String, Link> links) {
    this.requires = requires;
    this.links = links;
  }

  public Set<String> requires() {
    return requires;
  }

  public Map<String, Link> links() {
    return links;
  }

  public Optional<Link> findLink(String module) {
    return Optional.ofNullable(links.get(module));
  }
}
