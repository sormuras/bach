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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** A map of module source units. */
public final class SourceUnits {

  public static SourceUnits of() {
    return new SourceUnits(Map.of());
  }

  public SourceUnits with(Map<String, SourceUnit> units) {
    return new SourceUnits(units);
  }

  public SourceUnits with(SourceUnit additional, SourceUnit... additionals) {
    var map = new TreeMap<>(units);
    map.put(additional.name(), additional);
    for (var it : additionals) map.put(it.name(), it);
    return new SourceUnits(map);
  }

  private final Map<String, SourceUnit> units;

  public SourceUnits(Map<String, SourceUnit> units) {
    units.forEach(this::checkKeyEqualsModuleName);
    this.units = Map.copyOf(units);
  }

  private void checkKeyEqualsModuleName(String key, SourceUnit unit) {
    if (key.equals(unit.name())) return;
    var message = "Key '" + key + "' and unit name '" + unit.name() + "' aren't equal.";
    throw new IllegalArgumentException(message);
  }

  public Map<String, SourceUnit> units() {
    return units;
  }

  public Optional<SourceUnit> unit(String name) {
    return Optional.ofNullable(units().get(name));
  }

  public boolean isEmpty() {
    return units().isEmpty();
  }

  public Stream<String> toNames() {
    return units().keySet().stream().sorted();
  }

  public String toNames(String delimiter) {
    return toNames().collect(Collectors.joining(delimiter));
  }

  public Stream<SourceUnit> toUnits() {
    return units().values().stream();
  }


}
