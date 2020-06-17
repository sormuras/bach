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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;

/** A modular Java project descriptor. */
public final class Project {

  public static Project of(String name, String version) {
    return new Project(Basics.of(name, version), Structure.of(), MainSources.of());
  }

  private final Basics basics;
  private final Structure structure;
  private final MainSources main;

  public Project(Basics basics, Structure structure, MainSources main) {
    this.basics = basics;
    this.structure = structure;
    this.main = main;
  }

  public Basics basics() {
    return basics;
  }

  public Structure structure() {
    return structure;
  }

  public MainSources main() {
    return main;
  }

  public List<String> toStrings() {
    var list = new ArrayList<String>();
    list.add(String.format("project %s {", basics().name()));
    list.add(String.format("  version \"%s\";", basics().version()));

    var base = structure().base();
    list.add("");
    list.add("  // base");
    list.add("  //   .directory " + base.directory().toUri());
    list.add("  //   .libraries " + base.libraries().toUri());
    list.add("  //   .workspace " + base.workspace().toUri());

    var locators = new TreeSet<>(structure().locators().values());
    if (!locators.isEmpty()) {
      list.add("");
      for (var locator : locators) {
        list.add(String.format("  locates %s via", locator.module()));
        list.add(String.format("      \"%s\";", locator.uri()));
      }
    }

    list.add("}");
    return list;
  }

  public Optional<Locator> findLocator(String module) {
    return Optional.ofNullable(structure.locators().get(module));
  }

  public String toNameAndVersion() {
    return basics.name() + ' ' + basics.version();
  }

  public Project with(Basics basics) {
    return new Project(basics, structure, main);
  }

  public Project with(Structure structure) {
    return new Project(basics, structure, main);
  }

  public Project with(MainSources main) {
    return new Project(basics, structure, main);
  }

  public Project with(Version version) {
    return with(new Basics(basics().name(), version));
  }

  public Project with(Base base) {
    return with(new Structure(base, structure().locators()));
  }

  public Project with(Locator... locators) {
    var map = new TreeMap<>(structure().locators());
    List.of(locators).forEach(locator -> map.put(locator.module(), locator));
    return with(new Structure(structure().base(), map));
  }
}
