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

import de.sormuras.bach.util.Modules;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** A modular project structure consisting of realms and other components. */
public /*static*/ class Structure {

  private final List<Realm> realms;
  private final String mainRealm;
  private final Library library;

  public Structure(List<Realm> realms, String mainRealm, Library library) {
    this.realms = realms;
    this.mainRealm = mainRealm;
    this.library = library;
  }

  public List<Realm> realms() {
    return realms;
  }

  public String mainRealm() {
    return mainRealm;
  }

  public Library library() {
    return library;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Structure.class.getSimpleName() + "[", "]")
        .add("realms=" + realms)
        .add("mainRealm='" + mainRealm + "'")
        .add("library=" + library)
        .toString();
  }

  public Optional<Realm> findRealm(String name) {
    return realms.stream().filter(realm -> realm.name().equals(name)).findAny();
  }

  public Optional<Realm> toMainRealm() {
    return mainRealm == null ? Optional.empty() : findRealm(mainRealm);
  }

  public List<String> toRealmNames() {
    return realms.stream().map(Realm::name).collect(Collectors.toList());
  }

  public Set<String> toDeclaredModuleNames() {
    var names = realms.stream().flatMap(realm -> realm.units().stream()).map(Unit::name);
    return names.sorted().collect(Collectors.toCollection(TreeSet::new));
  }

  public Set<String> toRequiredModuleNames() {
    return Modules.required(realms.stream().flatMap(realm -> realm.units().stream()).map(Unit::descriptor));
  }
}
