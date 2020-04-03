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

package de.sormuras.bach;

import de.sormuras.bach.project.Structure;
import java.lang.module.ModuleDescriptor.Version;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/** A named and versioned modular Java project. */
public /*static*/ class Project {

  private final String name;
  private final Version version;
  private final Structure structure;

  public Project(String name, Version version, Structure structure) {
    this.name = name;
    this.version = version;
    this.structure = structure;
  }

  public String name() {
    return name;
  }

  public Version version() {
    return version;
  }

  public Structure structure() {
    return structure;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Project.class.getSimpleName() + "[", "]")
        .add("name='" + name + "'")
        .add("version=" + version)
        .add("structure=" + structure)
        .toString();
  }

  public String toNameAndVersion() {
    return name + ' ' + version;
  }

  public List<String> toStrings() {
    var strings = new ArrayList<String>();
    strings.add("Project " + toNameAndVersion());
    strings.add("\tRealms: " + structure.toRealmNames());
    for (var realm : structure.realms()) {
      strings.add("\t\tRealm \"" + realm.name() + '"');
      strings.add("\t\t\trelease=" + realm.release());
      strings.add("\t\t\tpreview=" + realm.preview());
      strings.add("\t\t\tUnits: [" + realm.units().size() + ']');
      for (var unit : realm.units()) {
        strings.add("\t\t\t\tUnit " + unit.descriptor().toNameAndVersion());
        strings.add("\t\t\t\t\tmain-class=" + unit.descriptor().mainClass().orElse("<empty>"));
        strings.add("\t\t\t\t\trequires=" + unit.toRequiresNames());
        strings.add("\t\t\t\t\tDirectories: [" + unit.directories().size() + ']');
        for (var directory : unit.directories()) {
          strings.add("\t\t\t\t\t\tpath=" + directory.path());
          strings.add("\t\t\t\t\t\trelease=" + directory.release());
        }
      }
    }
    return List.copyOf(strings);
  }
}
