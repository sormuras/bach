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

import de.sormuras.bach.tool.Javac;
import de.sormuras.bach.tool.Javadoc;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** A set of module units and tool arguments. */
public final class MainSources {

  public static MainSources of() {
    return new MainSources(Map.of(), Javac.of(), Javadoc.of());
  }

  private final Map<String, ModuleSource> units;
  private final Javac javac;
  private final Javadoc javadoc;

  public MainSources(Map<String, ModuleSource> units, Javac javac, Javadoc javadoc) {
    this.units = Map.copyOf(units);
    this.javac = javac;
    this.javadoc = javadoc;
  }

  public Map<String, ModuleSource> units() {
    return units;
  }

  public Javac javac() {
    return javac;
  }

  public Javadoc javadoc() {
    return javadoc;
  }

  public List<String> unitNames() {
    return units.keySet().stream().sorted().collect(Collectors.toUnmodifiableList());
  }

  public Optional<ModuleSource> unit(String name) {
    return Optional.ofNullable(units.get(name));
  }

  public MainSources with(ModuleSource unit, ModuleSource... more) {
    var mergedUnits = new TreeMap<>(units);
    mergedUnits.put(unit.module().name(), unit);
    for (var m : more) mergedUnits.put(m.module().name(), unit);
    return new MainSources(mergedUnits, javac, javadoc);
  }

  public MainSources with(Javac javac) {
    return new MainSources(units, javac, javadoc);
  }

  public MainSources with(Javadoc javadoc) {
    return new MainSources(units, javac, javadoc);
  }
}
