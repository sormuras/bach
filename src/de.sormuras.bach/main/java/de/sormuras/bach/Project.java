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

import de.sormuras.bach.internal.Factory;
import de.sormuras.bach.internal.Factory.Kind;
import de.sormuras.bach.internal.Modules;
import de.sormuras.bach.internal.Paths;
import de.sormuras.bach.internal.Scribe;
import de.sormuras.bach.project.Base;
import de.sormuras.bach.project.Library;
import de.sormuras.bach.project.Link;
import de.sormuras.bach.project.MainSources;
import de.sormuras.bach.project.SourceUnit;
import de.sormuras.bach.project.Sources;
import de.sormuras.bach.project.TestPreview;
import de.sormuras.bach.project.TestSources;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
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
  public static Project of() {
    return new Project(Base.of(), "unnamed", Version.parse("1-ea"), Sources.of(), Library.of());
  }

  @Factory
  public static Project ofCurrentDirectory() {
    return ofDirectory(Base.of());
  }

  @Factory
  public static Project ofDirectory(Base base) {
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
  public Project with(MainSources.Modifier... modifiers) {
    return sources(sources.mainSources(sources.mainSources().with(modifiers)));
  }

  @Factory(Kind.OPERATOR)
  public Project withMainSourcesCompiledForJavaRelease(int feature) {
    return sources(sources.mainSources(sources.mainSources().release(feature)));
  }

  @Factory(Kind.OPERATOR)
  public Project withMainSource(String path) {
    var unit = SourceUnit.of(Path.of(path));
    return sources(sources.mainSources(sources.mainSources().with(unit)));
  }

  @Factory(Kind.OPERATOR)
  public Project withMainSource(String path, int defaultJavaRelease) {
    var unit = SourceUnit.of(Path.of(path), defaultJavaRelease);
    return sources(sources.mainSources(sources.mainSources().with(unit)));
  }

  @Factory(Kind.OPERATOR)
  public Project withTestSource(String path) {
    var unit = SourceUnit.of(Path.of(path));
    return sources(sources.testSources(sources.testSources().with(unit)));
  }

  @Factory(Kind.OPERATOR)
  public Project withPreview(String path) {
    var unit = SourceUnit.of(Path.of(path));
    return sources(sources.testPreview(sources.testPreview().with(unit)));
  }

  @Factory(Kind.OPERATOR)
  public Project with(Link... moreLinks) {
    return library(library.with(moreLinks));
  }

  @Factory(Kind.OPERATOR)
  public Project withLibraryRequires(String... moreRequires) {
    return library(library.withRequires(moreRequires));
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

  public Path toModuleArchive(String realm, String module) {
    return toModuleArchive(realm, module, version);
  }

  public Path toModuleArchive(String realm, String module, Version version) {
    var suffix = realm.isEmpty() ? "" : '-' + realm;
    return base.modules(realm).resolve(module + '@' + version + suffix + ".jar");
  }

  public List<String> toStrings() {
    return new Scribe.Scroll("  ").add(this).toString().lines().collect(Collectors.toList());
  }
}
