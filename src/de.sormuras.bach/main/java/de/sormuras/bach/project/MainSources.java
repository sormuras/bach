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
import de.sormuras.bach.tool.Javac;
import de.sormuras.bach.tool.Javadoc;
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

/** A set of module units and tool arguments. */
public final class MainSources {

  public static MainSources of() {
    return new MainSources(Map.of(), Javac.of(), Javadoc.of());
  }

  private final Map<String, SourceUnit> units;
  private final Javac javac;
  private final Javadoc javadoc;

  public MainSources(Map<String, SourceUnit> units, Javac javac, Javadoc javadoc) {
    this.units = Map.copyOf(units);
    this.javac = javac;
    this.javadoc = javadoc;
  }

  public Map<String, SourceUnit> units() {
    return units;
  }

  public Javac javac() {
    return javac;
  }

  public Javadoc javadoc() {
    return javadoc;
  }

  public Optional<SourceUnit> unit(String name) {
    return Optional.ofNullable(units.get(name));
  }

  public List<String> toUnitNames() {
    return units.keySet().stream().sorted().collect(Collectors.toUnmodifiableList());
  }

  public List<String> toModuleSourcePaths() {
    var paths = new ArrayList<String>();
    var patterns = new TreeSet<String>(); // "src:etc/*/java"
    var specific = new TreeMap<String, List<Path>>(); // "foo=java:java-9"
    for (var unit : units().values()) {
      var sourcePaths = unit.toRelevantSourcePaths();
      try {
        for (var path : sourcePaths) patterns.add(Modules.modulePatternForm(path, unit.name()));
      } catch (FindException e) {
        specific.put(unit.name(), sourcePaths);
      }
    }
    if (patterns.isEmpty() && specific.isEmpty()) throw new IllegalStateException("");
    if (!patterns.isEmpty()) paths.add(String.join(File.pathSeparator, patterns));
    var entries = specific.entrySet();
    for (var entry : entries) paths.add(entry.getKey() + "=" + Paths.join(entry.getValue()));
    return List.copyOf(paths);
  }

  public MainSources with(SourceUnit unit, SourceUnit... more) {
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
