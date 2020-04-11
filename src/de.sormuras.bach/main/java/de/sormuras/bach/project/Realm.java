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

import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

/** A named collection of modular units sharing compilation-related properties. */
public /*static*/ class Realm {

  private final String name;
  private final int release;
  private final boolean preview;
  private final List<Unit> units;
  private final String mainUnit;

  public Realm(String name, int release, boolean preview, List<Unit> units, String mainUnit) {
    this.name = name;
    this.release = release;
    this.preview = preview;
    this.units = units;
    this.mainUnit = mainUnit;
  }

  public String name() {
    return name;
  }

  public int release() {
    return release;
  }

  public boolean preview() {
    return preview;
  }

  public List<Unit> units() {
    return units;
  }

  public String mainUnit() {
    return mainUnit;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Realm.class.getSimpleName() + "[", "]")
        .add("name='" + name + "'")
        .add("release=" + release)
        .add("preview=" + preview)
        .add("units=" + units)
        .add("mainUnit=" + mainUnit)
        .toString();
  }

  public Optional<Unit> toMainUnit() {
    return units.stream().filter(unit -> unit.name().equals(mainUnit)).findAny();
  }
}
