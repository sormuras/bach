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

import de.sormuras.bach.Scribe;
import de.sormuras.bach.internal.Factory;
import de.sormuras.bach.internal.Factory.Kind;
import de.sormuras.bach.internal.Modules;
import de.sormuras.bach.internal.Paths;
import java.io.File;
import java.lang.module.FindException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** A map of module source units. */
public final class SourceUnitMap implements Scribe {

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

  @Override
  public void scribe(Scroll scroll) {
    scroll.append("SourceUnitMap.of()");
    map.values().stream().sorted().forEach(unit -> scroll.addNewLine().add(".with", unit));
  }

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

  public List<String> toModuleSourcePaths(boolean forceModuleSpecificForm) {
    var paths = new ArrayList<String>();
    var patterns = new TreeSet<String>(); // "src:etc/*/java"
    var specific = new TreeMap<String, List<Path>>(); // "foo=java:java-9"
    for (var unit : map.values()) {
      var sourcePaths = unit.sources().toModuleSpecificSourcePaths();
      if (forceModuleSpecificForm) {
        specific.put(unit.name(), sourcePaths);
        continue;
      }
      try {
        for (var path : sourcePaths) {
          patterns.add(Modules.toModuleSourcePathPatternForm(path, unit.name()));
        }
      } catch (FindException e) {
        specific.put(unit.name(), sourcePaths);
      }
    }
    if (patterns.isEmpty() && specific.isEmpty()) throw new IllegalStateException("No forms?!");
    if (!patterns.isEmpty()) paths.add(String.join(File.pathSeparator, patterns));
    var entries = specific.entrySet();
    for (var entry : entries) paths.add(entry.getKey() + "=" + Paths.join(entry.getValue()));
    return List.copyOf(paths);
  }

  public Map<String, String> toModulePatches(SourceUnitMap upstream) {
    if (map.isEmpty() || upstream.isEmpty()) return Map.of();
    var patches = new TreeMap<String, String>();
    for (var unit : map.values()) {
      var module = unit.name();
      upstream
          .findUnit(module)
          .ifPresent(up -> patches.put(module, up.sources().toModuleSpecificSourcePath()));
    }
    return patches;
  }
}
