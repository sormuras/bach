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
import java.lang.module.ModuleDescriptor.Version;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Bach's project model. */
public final class Project {

  public static Project ofSystem() {
    var name = System.getProperty("bach.project.name", "unnamed");
    var version = System.getProperty("bach.project.version", "1-ea");
    return of(name, version);
  }

  public static Project of(String name, String version) {
    return of(name, Version.parse(version));
  }

  public static Project of(String name, Version version) {
    return new Project(Base.of(), name, version, Sources.of());
  }

  public Project with(Base base) {
    return new Project(base, name, version, sources);
  }

  public Project with(String name) {
    return new Project(base, name, version, sources);
  }

  public Project with(Version version) {
    return new Project(base, name, version, sources);
  }

  public Project with(Sources sources) {
    return new Project(base, name, version, sources);
  }

  private final Base base;
  private final String name;
  private final Version version;
  private final Sources sources;

  public Project(Base base, String name, Version version, Sources sources) {
    this.base = base;
    this.name = name;
    this.version = version;
    this.sources = sources;
  }

  public Base base() {
    return base;
  }

  public String name() {
    return name;
  }

  public Version version() {
    return version;
  }

  public Sources sources() {
    return sources;
  }

  public String toNameAndVersion() {
    return name + ' ' + version;
  }

  public List<String> toStrings() {
    var list = new ArrayList<String>();
    list.add("project " + name + '@' + version + " {");
    list.add("");
    list.add("  // base");
    list.add("  directory " + Paths.quote(base.directory()) + "; // " + base.directory().toUri());
    list.add("  libraries " + Paths.quote(base.libraries()) + ';');
    list.add("  workspace " + Paths.quote(base.workspace()) + ';');
    list.add("");
    list.add("  // main");
    list.add("  release " + sources.main().release().feature() + ';');
    toStrings(list, sources.main().units());
    list.add("");
    list.add("  // test");
    toStrings(list, sources.test().units());
    list.add("");
    list.add("  // test-preview");
    toStrings(list, sources.preview().units());
    list.add("}");
    return List.copyOf(list);
  }

  private void toStrings(List<String> list, SourceUnits units) {
    for (var unit : units.units().values()) {
      list.add("  " + unit.name());
      list.add("    module");
      list.add("      requires " + Modules.required(unit.descriptor()) + ';');
      unit.descriptor().mainClass().ifPresent(main -> list.add("      main-class " + main + ';'));
      list.add("    sources");
      for (var directory : unit.sources().directories()) {
        if (directory.isTargeted()) list.add("      release " + directory.release() + ';');
        var comment = directory.isModuleInfoJavaPresent() ? " // module-info.java" : "";
        list.add("      path " + Paths.quote(directory.path()) + ';' + comment);
      }
      if (!unit.resources().isEmpty()) {
        var quoted = unit.resources().stream().map(Paths::quote).sorted();
        list.add("    resources " + quoted.collect(Collectors.joining(", ")));
      }
    }
  }
}
