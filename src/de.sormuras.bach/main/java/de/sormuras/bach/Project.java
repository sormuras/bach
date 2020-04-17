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

import de.sormuras.bach.project.Information;
import de.sormuras.bach.project.Structure;
import de.sormuras.bach.project.Unit;
import java.lang.module.ModuleDescriptor.Version;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/** A named and versioned modular Java project. */
public /*static*/ class Project {

  private final String name;
  private final Version version;

  private final Information information;
  private final Structure structure;

  public Project(String name, Version version, Information information, Structure structure) {
    this.name = name;
    this.version = version;
    this.information = information;
    this.structure = structure;
  }

  public String name() {
    return name;
  }

  public Version version() {
    return version;
  }

  public Information information() {
    return information;
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

  public Version toModuleVersion(Unit unit) {
    return unit.descriptor().version().orElse(version);
  }

  public List<String> toStrings() {
    var strings = new ArrayList<String>();
    strings.add("Project");
    strings.add("\tname=\"" + name + '"');
    strings.add("\tversion=" + version);
    strings.add("Information");
    strings.add("\tdescription=\"" + information.description() + '"');
    strings.add("\turi=" + information.uri());
    strings.add("Structure");
    strings.add("\tDeclared modules: " + structure.toDeclaredModuleNames());
    strings.add("\tRequired modules: " + structure.toRequiredModuleNames());
    strings.add("\tRealms: " + structure.toRealmNames());
    structure.toMainRealm().ifPresent(realm -> strings.add("\tmain-realm=\"" + realm.name() + '"'));
    for (var realm : structure.realms()) {
      strings.add("\tRealm \"" + realm.name() + '"');
      strings.add("\t\trelease=" + realm.release());
      strings.add("\t\tpreview=" + realm.preview());
      realm.toMainUnit().ifPresent(unit -> strings.add("\t\tmain-unit=" + unit.name()));
      strings.add("\t\tUnits: [" + realm.units().size() + ']');
      for (var unit : realm.units()) {
        var module = unit.descriptor();
        strings.add("\t\tUnit \"" + module.toNameAndVersion() + '"');
        module.mainClass().ifPresent(it -> strings.add("\t\t\tmain-class=" + it));
        var requires = unit.toRequiresNames();
        if (!requires.isEmpty()) strings.add("\t\t\trequires=" + requires);
        strings.add("\t\t\tDirectories: [" + unit.directories().size() + ']');
        for (var directory : unit.directories()) {
          strings.add("\t\t\t" + directory);
        }
      }
    }
    strings.add("Library");
    strings.add("\trequires=" + structure.library().requires());
    return List.copyOf(strings);
  }
}
