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

import de.sormuras.bach.internal.Paths;
import de.sormuras.bach.tool.Javac;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** A set of module units and tool arguments for testing main modules. */
public final class TestSources {

  public static TestSources of() {
    return new TestSources(Map.of(), Javac.of());
  }

  private final Map<String, SourceUnit> units;
  private final Javac javac;

  public TestSources(Map<String, SourceUnit> units, Javac javac) {
    this.units = Map.copyOf(units);
    this.javac = javac;
  }

  public String name() {
    return "test";
  }

  public Map<String, SourceUnit> units() {
    return units;
  }

  public Javac javac() {
    return javac;
  }

  public Optional<SourceUnit> unit(String name) {
    return Optional.ofNullable(units.get(name));
  }

  public List<String> toUnitNames() {
    return units.keySet().stream().sorted().collect(Collectors.toUnmodifiableList());
  }

  public List<String> toModuleSourcePaths() {
    var paths = new ArrayList<String>();
    var specific = new TreeMap<String, List<Path>>(); // "foo=java:java-9"
    for (var unit : units().values()) {
      var sourcePaths = unit.toRelevantSourcePaths();
      specific.put(unit.name(), sourcePaths);
    }
    if (specific.isEmpty()) throw new IllegalStateException("");
    var entries = specific.entrySet();
    for (var entry : entries) paths.add(entry.getKey() + "=" + Paths.join(entry.getValue()));
    return List.copyOf(paths);
  }

  public Map<String, String> toModulePatchPaths(MainSources main) {
    if (units.isEmpty() || main.units().isEmpty()) return Map.of();
    var patches = new TreeMap<String, String>();
    for (var unit : units.values()) {
      var module = unit.name();
      main.units().values().stream()
          .filter(up -> up.name().equals(module))
          .findAny()
          .ifPresent(up -> patches.put(module, Paths.join(up.toRelevantSourcePaths())));
    }
    return patches;
  }

  public TestSources with(SourceUnit unit, SourceUnit... more) {
    var mergedUnits = new TreeMap<>(units);
    mergedUnits.put(unit.module().name(), unit);
    for (var m : more) mergedUnits.put(m.module().name(), unit);
    return new TestSources(mergedUnits, javac);
  }

  public TestSources with(Javac javac) {
    return new TestSources(units, javac);
  }
}
