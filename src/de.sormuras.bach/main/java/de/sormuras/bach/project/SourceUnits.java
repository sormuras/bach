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
public final class SourceUnits {

  public static SourceUnits of() {
    return new SourceUnits(Map.of());
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

  public List<String> toModuleSourcePaths(boolean forceModuleSpecificForm) {
    var paths = new ArrayList<String>();
    var patterns = new TreeSet<String>(); // "src:etc/*/java"
    var specific = new TreeMap<String, List<Path>>(); // "foo=java:java-9"
    for (var unit : units().values()) {
      var sourcePaths = unit.toRelevantSourcePaths();
      if (forceModuleSpecificForm) {
        specific.put(unit.name(), sourcePaths);
        continue;
      }
      try {
        for (var path : sourcePaths) patterns.add(Modules.modulePatternForm(path, unit.name()));
      } catch (FindException e) {
        specific.put(unit.name(), sourcePaths);
      }
    }
    if (patterns.isEmpty() && specific.isEmpty()) throw new IllegalStateException("Empty forms?!");
    if (!patterns.isEmpty()) paths.add(String.join(File.pathSeparator, patterns));
    var entries = specific.entrySet();
    for (var entry : entries) paths.add(entry.getKey() + "=" + Paths.join(entry.getValue()));
    return List.copyOf(paths);
  }

  public Map<String, String> toModulePatches(SourceUnits upstream) {
    if (units().isEmpty() || upstream.isEmpty()) return Map.of();
    var patches = new TreeMap<String, String>();
    for (var unit : units().values()) {
      var module = unit.name();
      upstream.units().values().stream()
          .filter(up -> up.name().equals(module))
          .findAny()
          .ifPresent(up -> patches.put(module, Paths.join(up.toRelevantSourcePaths())));
    }
    return patches;
  }

  public SourceUnits with(SourceUnit... units) {
    var map = new TreeMap<>(units());
    for (var unit : units) map.put(unit.name(), unit);
    return new SourceUnits(map);
  }
}
