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

package de.sormuras.bach.api;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/** A realm of modular sources. */
public /*static*/ final class Realm {

  /** Realm-related flags controlling the build process. */
  public enum Flag {
    ENABLE_PREVIEW,
    CREATE_JAVADOC,
    LAUNCH_TESTS
  }

  private final String name;
  private final int feature;
  private final Map<String, Unit> units;
  private final List<Realm> requires;
  private final Set<Flag> flags;

  public Realm(String name, int feature, Map<String, Unit> units, List<Realm> requires, Flag... flags) {
    this.name = Objects.requireNonNull(name, "name");
    this.feature = Objects.checkIndex(feature, Runtime.version().feature() + 1);
    this.units = Map.copyOf(units);
    this.requires = List.copyOf(requires);
    this.flags = flags.length == 0 ? Set.of() : EnumSet.copyOf(Set.of(flags));
  }

  public String name() {
    return name;
  }

  public int feature() {
    return feature;
  }

  public Map<String, Unit> units() {
    return units;
  }

  public List<Realm> requires() {
    return requires;
  }

  public Set<Flag> flags() {
    return flags;
  }

  public String title() {
    return name.isEmpty() ? "default" : name;
  }

  public OptionalInt release() {
    return feature == 0 ? OptionalInt.empty() : OptionalInt.of(feature);
  }

  /** Generate {@code --module-source-path} argument for this realm. */
  public String moduleSourcePath() {
    return units.values().stream()
        .map(Unit::moduleSourcePath)
        .distinct()
        .collect(Collectors.joining(File.pathSeparator));
  }

  /** Generate {@code --patch-module} value strings for this realm. */
  public List<String> patches(BiFunction<Realm, Unit, List<Path>> patcher) {
    FileSystems.getDefault().getSeparator();
    if (requires.isEmpty()) return List.of();
    var patches = new ArrayList<String>();
    for (var unit : units().values()) {
      var module = unit.name();
      for (var required : requires) {
        var other = required.units().get(module);
        if (other == null) continue;
        var paths =
            patcher.apply(required, other).stream()
                .map(Path::toString)
                .collect(Collectors.joining(File.pathSeparator));
        patches.add(module + '=' + paths);
      }
    }
    return patches.isEmpty() ? List.of() : List.copyOf(patches);
  }
}
