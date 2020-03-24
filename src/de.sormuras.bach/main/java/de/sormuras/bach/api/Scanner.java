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

import de.sormuras.bach.Convention;
import de.sormuras.bach.internal.Modules;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** An extensible project builder factory. */
public /*static*/ class Scanner {

  private final Paths paths;

  public Scanner(Paths paths) {
    this.paths = paths;
  }

  public Path base() {
    return paths.base();
  }

  /** Scan base directory for project components. */
  public Project.Builder scan() {
    var units = scanUnits();
    var requires = Modules.required(units.stream().map(Unit::descriptor));
    var layout = Layout.find(units).orElseThrow();
    return Project.builder()
        .paths(paths)
        .name(scanName().orElse("unnamed"))
        .units(units)
        .realms(layout.realmsOf(units))
        .requires(scanRequires(requires))
        .locators(List.of(Locator.dynamicCentral(Map.of()), Locator.sormurasModules(Map.of())));
  }

  /** Return name of the project. */
  public Optional<String> scanName() {
    return Optional.ofNullable(base().toAbsolutePath().getFileName())
        .map(Object::toString)
        .map(name -> name.replace(' ', '.'))
        .map(name -> name.replace('-', '.'))
        .map(name -> name.replace('_', '.'));
  }

  /** Scan for modular source units. */
  public List<Unit> scanUnits() {
    var base = paths.base();
    var src = base.resolve("src"); // More subdirectory candidates? E.g. "modules", "sources"?
    var root = Files.isDirectory(src) ? src : base;
    try (var stream = Files.find(root, 9, (path, __) -> path.endsWith("module-info.java"))) {
      var paths = stream.collect(Collectors.toCollection(ArrayList::new));
      var count = paths.stream().mapToInt(Path::getNameCount).min().orElseThrow();
      return paths.stream()
          .filter(path -> path.getNameCount() == count)
          .map(this::scanUnit)
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** Return a unit for the given info. */
  public Unit scanUnit(Path info) {
    var parent = info.getParent();
    var resources = parent.resolveSibling("resources");
    return new Unit(
        info,
        Modules.describe(info),
        scanUnitSources(parent),
        Files.isDirectory(resources) ? List.of(resources) : List.of());
  }

  /** Scan the parent directory of a {@code module-info.java} file for sources. */
  public List<Source> scanUnitSources(Path path) {
    if (path.getFileName().toString().matches("java-\\d+")) {
      var sources = new ArrayList<Source>();
      for (int feature = 7; feature <= Runtime.version().feature(); feature++) {
        var javaN = path.resolveSibling("java-" + feature);
        if (Files.notExists(javaN)) continue; // feature
        var source =
            new Source(
                javaN, feature, sources.isEmpty() ? Set.of() : Set.of(Source.Flag.VERSIONED));
        sources.add(source);
      }
      return List.copyOf(sources);
    }
    return List.of(Source.of(path));
  }

  public Set<String> scanRequires(Set<String> requires) {
    var modules = new TreeSet<>(requires);
    Convention.amendJUnitTestEngines(modules);
    Convention.amendJUnitPlatformConsole(modules);
    return modules;
  }

  /** Source directory tree layout. */
  interface Layout {

    /** Return name of the realm for the given modular unit; may be {@code null}. */
    String realmOf(Unit unit);

    /** Return realms based on the given units. */
    List<Realm> realmsOf(List<Unit> units);

    /** Scan the given units for a well-known modular source directory tree layout. */
    static Optional<Layout> find(Collection<Unit> units) {
      if (units.isEmpty()) return Optional.empty();
      var layouts = List.of(new MainTest(), new Default());
      for (var layout : layouts)
        if (units.stream().allMatch(unit -> Objects.nonNull(layout.realmOf(unit))))
          return Optional.of(layout);
      return Optional.empty();
    }

    /** Default (realm with an empty name: {@code ""}) source directory layout. */
    final class Default implements Layout {
      @Override
      public String realmOf(Unit unit) {
        return "";
      }

      @Override
      public List<Realm> realmsOf(List<Unit> units) {
        var release = Convention.javaReleaseStatistics(Unit.paths(units)).getMax();
        var main = Convention.mainModule(units.stream().map(Unit::descriptor)).orElse(null);
        var flags = new Realm.Flag[] {Realm.Flag.CREATE_JAVADOC, Realm.Flag.CREATE_IMAGE};
        return List.of(new Realm("", release, units, main, List.of(), flags));
      }
    }

    /** Source directory layout with main and test realms and a nested category. */
    final class MainTest implements Layout {
      @Override
      public String realmOf(Unit unit) {
        var info = unit.info();
        var deque = new ArrayDeque<String>();
        info.forEach(path -> deque.addFirst(path.toString()));
        var found = deque.pop().equals("module-info.java");
        if (!found) throw new IllegalArgumentException("No module-info.java?! " + info);
        var category = deque.pop();
        if (category.equals("java") || category.equals("module") || category.matches("java-\\d+")) {
          var realm = deque.pop();
          if (realm.equals("main") || realm.equals("test")) return realm;
        }
        return null;
      }

      @Override
      public List<Realm> realmsOf(List<Unit> units) {
        if (units.isEmpty()) return List.of();
        var mainUnits = new ArrayList<Unit>();
        var testUnits = new ArrayList<Unit>();
        var uncharted = new TreeSet<String>();
        for (var unit : units) {
          var realm = realmOf(unit);
          if (realm == null) throw new IllegalArgumentException("Unknown realm: " + unit);
          if (realm.equals("main")) {
            mainUnits.add(unit);
            continue;
          }
          if (realm.equals("test")) {
            testUnits.add(unit);
            continue;
          }
          uncharted.add(realm);
        }
        if (!uncharted.isEmpty()) throw new IllegalArgumentException("Realms? " + uncharted);
        var realms = new ArrayList<Realm>();
        if (!mainUnits.isEmpty())
          realms.add(
              new Realm(
                  "main",
                  Convention.javaReleaseStatistics(Unit.paths(mainUnits)).getMax(),
                  mainUnits,
                  Convention.mainModule(mainUnits.stream().map(Unit::descriptor)).orElse(null),
                  List.of(),
                  Realm.Flag.CREATE_JAVADOC,
                  Realm.Flag.CREATE_IMAGE));
        if (!testUnits.isEmpty())
          realms.add(
              new Realm(
                  "test",
                  Convention.javaReleaseStatistics(Unit.paths(testUnits)).getMax(),
                  testUnits,
                  null,
                  realms,
                  Realm.Flag.LAUNCH_TESTS));
        if (realms.isEmpty())
          throw new IllegalArgumentException("No main nor test units: " + units);
        return realms;
      }
    }
  }
}
