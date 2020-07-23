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

import de.sormuras.bach.internal.Factory;
import de.sormuras.bach.internal.Factory.Kind;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** An external modules manager backed by the {@link Base#libraries() lib} directory. */
public final class Library {

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

  //
  // Configuration API
  //

  @Factory
  public static Library of() {
    return new Library(Set.of(), Map.of());
  }

  @Factory(Kind.SETTER)
  public Library requires(Set<String> requires) {
    return new Library(requires, links);
  }

  @Factory(Kind.SETTER)
  public Library links(Map<String, Link> links) {
    return new Library(requires, links);
  }

  @Factory(Kind.OPERATOR)
  public Library withRequires(String... moreModuleNames) {
    var set = new TreeSet<>(requires);
    set.addAll(Arrays.asList(moreModuleNames));
    return requires(set);
  }

  @Factory(Kind.OPERATOR)
  public Library withLink(String module, String uri) {
    return with(Link.of(module, uri));
  }

  @Factory(Kind.OPERATOR)
  public Library with(Link... moreLinks) {
    var merged = new TreeMap<>(links);
    for (var link : moreLinks) merged.put(link.module(), link);
    return links(merged);
  }

  //
  // Normal API
  //

  public Optional<Link> findLink(String module) {
    return Optional.ofNullable(links.get(module));
  }

  public Set<String> toRequiredModuleNames() {
    return new TreeSet<>(requires);
  }
}
