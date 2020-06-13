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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

/** A modular Java project descriptor. */
public final class Project {

  public static Project of(String name, String version) {
    return new Project(Basics.of(name, version), Structure.of());
  }

  private final Basics basics;
  private final Structure structure;

  public Project(Basics basics, Structure structure) {
    this.basics = basics;
    this.structure = structure;
  }

  public Basics basics() {
    return basics;
  }

  public Structure structure() {
    return structure;
  }

  public Optional<Locator> findLocator(String module) {
    return Optional.ofNullable(structure.locators().get(module));
  }

  public String toNameAndVersion() {
    return basics.name() + ' ' + basics.version();
  }

  public Project with(Basics basics) {
    return new Project(basics, structure);
  }

  public Project with(Structure structure) {
    return new Project(basics, structure);
  }

  public Project with(Version version) {
    return with(new Basics(basics().name(), version));
  }

  public Project with(Paths paths) {
    return with(new Structure(paths, structure().locators()));
  }

  public Project with(Locator... locators) {
    return with(List.of(locators));
  }

  public Project with(Collection<Locator> locators) {
    var map = new TreeMap<>(structure().locators());
    locators.forEach(locator -> map.put(locator.module(), locator));
    return with(new Structure(structure().paths(), map));
  }
}
