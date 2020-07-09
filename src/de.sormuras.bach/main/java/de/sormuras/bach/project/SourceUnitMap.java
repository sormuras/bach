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
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** A map of module source units. */
public final class SourceUnitMap {

  private final Map<String, SourceUnit> map;

  public SourceUnitMap(Map<String, SourceUnit> map) {
    this.map = Map.copyOf(map);
  }

  public Map<String, SourceUnit> map() {
    return map;
  }

  //
  // Configuration API
  //

  @Factory
  public static SourceUnitMap of() {
    return new SourceUnitMap(Map.of());
  }

  @Factory(Kind.SETTER)
  public SourceUnitMap map(Map<String, SourceUnit> map) {
    return new SourceUnitMap(map);
  }

  @Factory(Kind.OPERATOR)
  public SourceUnitMap with(SourceUnit... moreUnits) {
    var merged = new TreeMap<>(map);
    for (var unit : moreUnits) merged.put(unit.name(), unit);
    return map(merged);
  }

  //
  // Normal API
  //

  public Optional<SourceUnit> findUnit(String name) {
    return Optional.ofNullable(map.get(name));
  }

  public boolean isEmpty() {
    return map.isEmpty();
  }

  public boolean isPresent() {
    return map.size() >= 1;
  }

  public int size() {
    return map.size();
  }

  public Stream<String> toNames() {
    return map.keySet().stream().sorted();
  }

  public String toNames(String delimiter) {
    return toNames().collect(Collectors.joining(delimiter));
  }

  public Stream<SourceUnit> toUnits() {
    return map.values().stream();
  }
}
