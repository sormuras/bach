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

import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public /*record*/ class Project {

  private final String name;
  private final String group;
  private final Version version;
  private final Structure structure;

  public Project(String name, String group, Version version, Structure structure) {
    this.name = name;
    this.group = group;
    this.version = version;
    this.structure = structure;
  }

  public String name() {
    return name;
  }

  public String group() {
    return group;
  }

  public Version version() {
    return version;
  }

  public Structure structure() {
    return structure;
  }

  public Folder folder() {
    return structure.folder();
  }

  public Optional<Unit> unit(String realmName, String unitName) {
    for (var realm : structure.realms()) {
      if (realm.name().equals(realmName)) {
        for (var unit : structure.units()) {
          if (unit.name().equals(unitName)) {
            return Optional.of(unit);
          }
        }
      }
    }
    return Optional.empty();
  }

  public List<Unit> units(Realm realm) {
    return structure().units().stream()
        .filter(unit -> unit.realm() == realm)
        .collect(Collectors.toList());
  }

  public Path modularJar(Unit unit) {
    var jar = unit.name() + '-' + version(unit) + ".jar";
    return folder().modules(unit.realm().name(), jar);
  }

  public Path sourcesJar(Unit unit) {
    var jar = unit.name() + '-' + version(unit) + "-sources.jar";
    return folder().deploy(unit, jar);
  }

  public Path javadocJar(Realm realm) {
    var jar = name + '-' + version + "-javadoc.jar";
    return folder().deploy(realm.name(), jar);
  }

  public Version version(Unit unit) {
    return unit.descriptor().version().orElse(version);
  }
}
