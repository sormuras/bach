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
import de.sormuras.bach.tool.Jar;
import de.sormuras.bach.tool.Javac;
import de.sormuras.bach.tool.Javadoc;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** A modular Java project descriptor. */
public final class Project {

  public static Project of(String name, String version) {
    var basics = Basics.of(name, version);
    return new Project(basics, Structure.of(), MainSources.of(), TestSources.of());
  }

  private final Basics basics;
  private final Structure structure;
  private final MainSources main;
  private final TestSources test;

  public Project(Basics basics, Structure structure, MainSources main, TestSources test) {
    this.basics = basics;
    this.structure = structure;
    this.main = main;
    this.test = test;
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

  public TestSources test() {
    return test;
  }

  public boolean isMainSourcePresent() {
    return !main().units().isEmpty();
  }

  public boolean isTestSourcePresent() {
    return !test().units().isEmpty();
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
    list.add("  // modules");
    list.add("  //    declares " + toDeclaredModuleNames());
    list.add("  //    requires " + toRequiredModuleNames());
    list.add("  //    external " + toExternalModuleNames());

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

  public Set<String> toDeclaredModuleNames() {
    return toUnits().map(SourceUnit::name).collect(Collectors.toCollection(TreeSet::new));
  }

  public Set<String> toExternalModuleNames() {
    var externals = new TreeSet<>(toRequiredModuleNames());
    externals.removeAll(toDeclaredModuleNames());
    externals.removeAll(Modules.declared(ModuleFinder.ofSystem()));
    return externals;
  }

  public Set<String> toRequiredModuleNames() {
    return Modules.required(toUnits().map(SourceUnit::module));
  }

  public Stream<SourceUnit> toUnits() {
    return Stream.concat(main().units().toUnits(), test().units().toUnits());
  }

  public Project with(Basics basics) {
    return new Project(basics, structure, main, test);
  }

  public Project with(Structure structure) {
    return new Project(basics, structure, main, test);
  }

  public Project with(MainSources main) {
    return new Project(basics, structure, main, test);
  }

  public Project with(TestSources test) {
    return new Project(basics, structure, main, test);
  }

  public Project with(Version version) {
    return with(basics().with(version));
  }

  public Project with(JavaRelease release) {
    return with(basics.with(release));
  }

  public Project with(Base base) {
    return with(new Structure(base, structure().requires(), structure().locators()));
  }

  public Project withRequires(String module, String... more) {
    var set = new TreeSet<>(structure().requires());
    set.add(module);
    Collections.addAll(set, more);
    return with(new Structure(structure().base(), set, structure().locators()));
  }

  public Project with(Locator... locators) {
    var map = new TreeMap<>(structure().locators());
    List.of(locators).forEach(locator -> map.put(locator.module(), locator));
    return with(new Structure(structure().base(), structure().requires(), map));
  }

  public Project withBaseDirectory(String first, String... more) {
    return with(Base.of(first, more));
  }

  public Project withSources() {
    var mains = new ArrayList<Path>();
    var tests = new ArrayList<Path>();
    var previews = new ArrayList<Path>();
    for (var info : Paths.findModuleInfoJavaFiles(structure().base().directory(), 9)) {
      if (Paths.isModuleInfoJavaFileForRealm(info, "test")) tests.add(info);
      else if (Paths.isModuleInfoJavaFileForRealm(info, "test-preview")) previews.add(info);
      else mains.add(info);
    }
    if (!previews.isEmpty()) throw new IllegalStateException("'test-preview' not supported, yet");
    return withMainSources(mains).withTestSources(tests);
  }

  public Project withMainSources(List<Path> mainInfoFiles) {
    var project = this;
    var release = basics().release().feature();
    var version = basics().version();
    var base = structure().base();
    var main = main();
    for (var info : mainInfoFiles) {
      var unit = SourceUnit.of(info);
      var module = unit.name();
      var file = module + '@' + version + ".jar";
      var jar =
          Jar.of(base.modules("").resolve(file))
              // .with("--verbose")
              // if (jarModuleWithSources) arguments.add("-C", sources0.path(), ".");
              .withChangeDirectoryAndIncludeFiles(base.classes("", release, module), ".");
      project = project.with(main = main.with(unit.with(jar)));
    }

    // pre-compute some arguments
    var releases = Runtime.version().feature() == release ? List.of() : List.of(release);
    var moduleSourcePaths = main.units().toModuleSourcePaths(false);

    // generate javac call
    var javac =
        Javac.of()
            .with("-d", base.classes("", release))
            .with(releases, (tool, value) -> tool.with("--release", value))
            .with("--module", main.units().toNames(","))
            .with("--module-version", version)
            .with(moduleSourcePaths, (tool, value) -> tool.with("--module-source-path", value));
    project = project.with(main = main.with(javac));

    // generate javadoc call
    var javadoc =
        Javadoc.of()
            .with("-d", base.documentation("api"))
            .with("--module", main.units().toNames(","))
            .with(moduleSourcePaths, (tool, value) -> tool.with("--module-source-path", value));

    return project.with(main.with(javadoc));
  }

  public Project withTestSources(List<Path> testInfoFiles) {
    if (testInfoFiles.isEmpty()) return with(TestSources.of());
    var project = this;
    var release = Runtime.version().feature();
    var version = Version.parse(basics().version().toString() + "-test");
    var base = structure().base();
    var test = TestSources.of();
    for (var info : testInfoFiles) {
      var unit = SourceUnit.of(info);
      var module = unit.name();
      var file = module + '@' + version + ".jar";
      var jar =
          Jar.of(base.modules("test").resolve(file))
              // .with("--verbose")
              .withChangeDirectoryAndIncludeFiles(base.classes("test", release, module), ".");
      test = test.with(unit.with(jar));
    }
    // pre-compute some arguments
    var modulePath = Paths.join(List.of(base.modules("")));
    var moduleSourcePaths = test.units().toModuleSourcePaths(true);
    var modulePatches = test.units().toModulePatches(main.units());
    // generate javac call
    var javac =
        Javac.of()
            .with("-d", base.classes("test", release))
            .with("--module", test.units().toNames(","))
            .with("--module-version", version)
            .with("--module-path", modulePath)
            .with(moduleSourcePaths, (tool, value) -> tool.with("--module-source-path", value))
            .with(
                modulePatches.entrySet(),
                (tool, patch) ->
                    tool.with("--patch-module", patch.getKey() + '=' + patch.getValue()));
    test = test.with(javac);
    return project.with(test);
  }
}
