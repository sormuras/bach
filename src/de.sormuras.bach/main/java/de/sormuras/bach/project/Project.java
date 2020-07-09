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

import de.sormuras.bach.internal.Factory;
import de.sormuras.bach.internal.Factory.Kind;
import de.sormuras.bach.internal.Modules;
import de.sormuras.bach.internal.Paths;
import java.lang.module.ModuleDescriptor.Version;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Bach's project model. */
public final class Project {

  private final Base base;
  private final String name;
  private final Version version;
  private final Sources sources;
  private final Library library;

  public Project(Base base, String name, Version version, Sources sources, Library library) {
    this.base = base;
    this.name = name;
    this.version = version;
    this.sources = sources;
    this.library = library;
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

  public Library library() {
    return library;
  }

  //
  // Configuration API
  //

  @Factory
  public static Project ofSystem() {
    var name = System.getProperty("bach.project.name", "unnamed");
    var version = System.getProperty("bach.project.version", "1-ea");
    return of(name, version);
  }

  @Factory
  public static Project of(String name, String version) {
    return of(name, Version.parse(version));
  }

  @Factory
  public static Project of(String name, Version version) {
    return new Project(Base.of(), name, version, Sources.of(), Library.of());
  }

  @Factory
  public static Project of(Base base) {
    var mainSources = MainSources.of();
    var testSources = TestSources.of();
    var testPreview = TestPreview.of();
    for (var info : Paths.findModuleInfoJavaFiles(base.directory(), 9)) {
      if (info.startsWith(".bach")) continue;
      var unit = SourceUnit.of(info);
      if (Paths.isModuleInfoJavaFileForRealm(info, "test")) {
        testSources = testSources.with(unit);
        continue;
      }
      if (Paths.isModuleInfoJavaFileForRealm(info, "test-preview")) {
        testPreview = testPreview.with(unit);
        continue;
      }
      mainSources = mainSources.with(unit);
    }
    var sources = new Sources(mainSources, testSources, testPreview);
    var absolute = base.directory().toAbsolutePath();
    var name = System.getProperty("bach.project.name", Paths.name(absolute).toLowerCase());
    var version = System.getProperty("bach.project.version", "1-ea");
    return new Project(base, name, Version.parse(version), sources, Library.of());
  }

  @Factory(Kind.SETTER)
  public Project base(Base base) {
    return new Project(base, name, version, sources, library);
  }

  @Factory(Kind.SETTER)
  public Project name(String name) {
    return new Project(base, name, version, sources, library);
  }

  @Factory(Kind.SETTER)
  public Project version(Version version) {
    return new Project(base, name, version, sources, library);
  }

  @Factory(Kind.SETTER)
  public Project version(String version) {
    return version(Version.parse(version));
  }

  @Factory(Kind.SETTER)
  public Project sources(Sources sources) {
    return new Project(base, name, version, sources, library);
  }

  @Factory(Kind.SETTER)
  public Project library(Library library) {
    return new Project(base, name, version, sources, library);
  }

  @Factory(Kind.OPERATOR)
  public Project withCompileMainSourcesForJavaRelease(int feature) {
    return sources(sources.mainSources(sources.mainSources().release(feature)));
  }

  @Factory(Kind.OPERATOR)
  public Project withMainSource(String path) {
    return sources(sources.mainSources(sources.mainSources().with(SourceUnit.of(path))));
  }

  @Factory(Kind.OPERATOR)
  public Project withTestSource(String path) {
    return sources(sources.testSources(sources.testSources().with(SourceUnit.of(path))));
  }

  @Factory(Kind.OPERATOR)
  public Project withPreview(String path) {
    return sources(sources.testPreview(sources.testPreview().with(SourceUnit.of(path))));
  }

  //
  // Normal API
  //

  public String toNameAndVersion() {
    return name + ' ' + version;
  }

  public Set<String> toDeclaredModuleNames() {
    return toUnits().map(SourceUnit::name).collect(Collectors.toCollection(TreeSet::new));
  }

  public Set<String> toExternalModuleNames() {
    return Modules.external(toDeclaredModuleNames(), toRequiredModuleNames());
  }

  public Set<String> toRequiredModuleNames() {
    return Modules.required(toUnits().map(SourceUnit::descriptor));
  }

  public Stream<SourceUnit> toUnits() {
    return Stream.concat(
        sources.mainSources().units().toUnits(),
        Stream.concat(
            sources.testSources().units().toUnits(), sources.testPreview().units().toUnits()));
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
    list.add("  release " + sources.mainSources().release().feature() + ';');
    toStrings(list, sources.mainSources().units());
    list.add("");
    list.add("  // test");
    toStrings(list, sources.testSources().units());
    list.add("");
    list.add("  // test-preview");
    toStrings(list, sources.testPreview().units());
    list.add("");
    list.add("  // modules");
    list.add("  //   declared " + toDeclaredModuleNames());
    list.add("  //   required " + toRequiredModuleNames());
    list.add("  //   external " + toExternalModuleNames());
    list.add("");
    list.add("  // library");
    list.add("  requires " + library.requires() + ';');
    for (var link : library.links().values()) {
      list.add("  links " + link.module() + " to");
      list.add("    " + link.uri() + ';');
    }
    list.add("}");
    return List.copyOf(list);
  }

  private void toStrings(List<String> list, SourceUnitMap sum) {
    for (var unit : sum.map().values()) {
      list.add("  " + unit.name());
      list.add("    module");
      list.add("      requires " + Modules.required(unit.descriptor()) + ';');
      unit.descriptor().mainClass().ifPresent(main -> list.add("      main-class " + main + ';'));
      list.add("    sources");
      for (var directory : unit.directories()) {
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
