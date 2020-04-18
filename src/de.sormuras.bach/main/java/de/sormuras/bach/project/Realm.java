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

import de.sormuras.bach.tool.JavaCompiler;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/** A named collection of modular units sharing compilation-related properties. */
public /*static*/ class Realm {

  private final String name;
  private final List<Unit> units;
  private final String mainUnit;
  private final List<Realm> upstreams;
  private final JavaCompiler javac;

  public Realm(String name, List<Unit> units, String mainUnit, List<Realm> upstreams, JavaCompiler javac) {
    this.name = name;
    this.units = units;
    this.mainUnit = mainUnit;
    this.upstreams = upstreams;
    this.javac = javac;
  }

  public String name() {
    return name;
  }

  public int release() {
    return javac.release();
  }

  public boolean preview() {
    return javac.preview();
  }

  public List<Unit> units() {
    return units;
  }

  public String mainUnit() {
    return mainUnit;
  }

  public List<Realm> upstreams() {
    return upstreams;
  }

  public JavaCompiler javac() {
    return javac;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Realm.class.getSimpleName() + "[", "]")
        .add("name='" + name + "'")
        .add("release=" + release())
        .add("preview=" + preview())
        .add("units=" + units)
        .add("mainUnit=" + mainUnit)
        .add("upstreams=" + upstreams)
        .add("javac=" + javac)
        .toString();
  }

  public Optional<Unit> toMainUnit() {
    return units.stream().filter(unit -> unit.name().equals(mainUnit)).findAny();
  }

  public List<String> toUpstreamNames() {
    return upstreams.stream().map(Realm::name).collect(Collectors.toList());
  }

  /** Find a unit instance by its name. */
  public Optional<Unit> findUnit(String name) {
    return units.stream().filter(unit -> unit.name().equals(name)).findAny();
  }

  /** Generate {@code --patch-module} value strings for this realm. */
  public Map<String, List<Path>> patches(BiFunction<Realm, Unit, List<Path>> patcher) {
    if (units.isEmpty() || upstreams.isEmpty()) return Map.of();
    var patches = new TreeMap<String, List<Path>>();
    for (var unit : units()) {
      var module = unit.name();
      for (var required : upstreams) {
        var other = required.findUnit(module);
        if (other.isEmpty()) continue;
        var paths = patcher.apply(required, other.orElseThrow());
        if (paths.isEmpty()) continue;
        patches.put(module, paths);
      }
    }
    return patches;
  }
}
