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

import de.sormuras.bach.project.structure.Realm;
import de.sormuras.bach.project.structure.Unit;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/** A modular project structure consisting of realms and other components. */
public /*static*/ class Structure {

  private final List<Realm> realms;
  private final String mainRealm;

  public Structure(List<Realm> realms, String mainRealm) {
    this.realms = realms;
    this.mainRealm = mainRealm;
  }

  public List<Realm> realms() {
    return realms;
  }

  public String mainRealm() {
    return mainRealm;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Structure.class.getSimpleName() + "[", "]")
        .add("realms=" + realms)
        .add("mainRealm='" + mainRealm + "'")
        .toString();
  }

  public Optional<Realm> toMainRealm() {
    return realms.stream().filter(realm -> realm.name().equals(mainRealm)).findAny();
  }

  public List<String> toRealmNames() {
    return realms.stream().map(Realm::name).collect(Collectors.toList());
  }

  public List<String> toUnitNames() {
    var names = realms.stream().flatMap(realm -> realm.units().stream()).map(Unit::name);
    return names.distinct().sorted().collect(Collectors.toList());
  }
}
