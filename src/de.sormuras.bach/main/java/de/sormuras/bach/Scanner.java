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

import de.sormuras.bach.internal.Modules;
import de.sormuras.bach.internal.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** A builder for building {@link Project.Builder} objects by parsing a directory for modules. */
public class Scanner {

  /** A layout defines a directory pattern for organizing {@code module-info.java} files. */
  public enum Layout {
    /** Detect layout on-the-fly. */
    AUTOMATIC,
    /** Single unnamed realm. */
    DEFAULT,
    /** By Three They Come: {@code main}, {@code test}, and {@code test-preview}. */
    MAIN_TEST_PREVIEW
  }

  private Project.Base base = Project.Base.of();
  private List<Path> moduleInfoFiles = new ArrayList<>();
  private int limit = 9;
  private Path offset = Path.of("");
  private Layout layout = Layout.AUTOMATIC;

  public Scanner base(String directory, String... more) {
    return base(Path.of(directory, more));
  }

  public Scanner base(Path directory) {
    return base(Project.Base.of(directory));
  }

  public Scanner base(Project.Base base) {
    this.base = base;
    return this;
  }

  public Scanner moduleInfoFiles(List<Path> moduleInfoFiles) {
    this.moduleInfoFiles = moduleInfoFiles;
    return this;
  }

  public Scanner offset(String offset, String... more) {
    return offset(Path.of(offset, more));
  }

  public Scanner offset(Path offset) {
    this.offset = offset;
    return this;
  }

  public Scanner limit(int limit) {
    this.limit = limit;
    return this;
  }

  public Scanner layout(Layout layout) {
    this.layout = layout;
    return this;
  }

  public Project.Builder newBuilder() {
    if (moduleInfoFiles.isEmpty()) {
      var directory = base.directory().resolve(offset);
      if (Paths.isRoot(directory)) throw new IllegalStateException("Root directory: " + directory);
      var paths = Paths.find(List.of(directory), limit, this::isModuleInfoJavaFile);
      if (paths.isEmpty()) throw new IllegalStateException("No module-info.java: " + directory);
      moduleInfoFiles(paths);
    }
    var directoryName = base.directory().toAbsolutePath().getFileName();
    var builder = Project.builder();
    builder.setBase(base);
    if (directoryName != null) builder.title(directoryName.toString());
    builder.setRealms(computeRealms());
    return builder;
  }

  boolean isModuleInfoJavaFile(Path path) {
    return Paths.isModuleInfoJavaFile(path) && !path.getName(0).toString().equals(".bach");
  }

  List<Project.Realm> computeRealms() {
    if (layout == Layout.DEFAULT) return computeUnnamedRealm();
    if (layout == Layout.MAIN_TEST_PREVIEW) return computeMainTestPreviewRealms();
    if (layout != Layout.AUTOMATIC) throw new AssertionError("Unexpected layout: " + layout);
    try {
      return computeMainTestPreviewRealms();
    } catch (UnsupportedOperationException ignored) {
    }
    return computeUnnamedRealm();
  }

  List<Project.Realm> computeUnnamedRealm() {
    return List.of(
        new RealmBuilder("")
            .takeMatchingModuleInfoFilesFrom(new ArrayList<>(moduleInfoFiles))
            .flag(Project.Realm.Flag.CREATE_API_DOCUMENTATION)
            .flag(Project.Realm.Flag.CREATE_CUSTOM_RUNTIME_IMAGE)
            .flag(Project.Realm.Flag.LAUNCH_TESTS)
            .build());
  }

  List<Project.Realm> computeMainTestPreviewRealms() {
    var files = new ArrayList<>(moduleInfoFiles);
    var main =
        new RealmBuilder("main")
            .takeMatchingModuleInfoFilesFrom(files)
            .flag(Project.Realm.Flag.CREATE_API_DOCUMENTATION)
            .flag(Project.Realm.Flag.CREATE_CUSTOM_RUNTIME_IMAGE)
            .build();
    var test =
        new RealmBuilder("test")
            .takeMatchingModuleInfoFilesFrom(files)
            .flag(Project.Realm.Flag.LAUNCH_TESTS)
            .upstream("main")
            .build();
    var preview =
        new RealmBuilder("test-preview")
            .takeMatchingModuleInfoFilesFrom(files)
            .flag(Project.Realm.Flag.ENABLE_PREVIEW_LANGUAGE_FEATURES)
            .flag(Project.Realm.Flag.LAUNCH_TESTS)
            .upstream("main")
            .upstream("test")
            .build();
    if (!files.isEmpty()) throw new UnsupportedOperationException("File(s) not taken: " + files);
    var realms = new ArrayList<Project.Realm>();
    if (!main.units().isEmpty()) realms.add(main);
    if (!test.units().isEmpty()) realms.add(test);
    if (!preview.units().isEmpty()) realms.add(preview);
    if (realms.isEmpty()) throw new UnsupportedOperationException("No match in: " + files);

    return List.copyOf(realms);
  }

  /** A builder for building {@link Project.Realm} objects. */
  private static class RealmBuilder {
    final String name;
    final List<Path> moduleInfoFiles = new ArrayList<>();
    final Set<Project.Realm.Flag> flags = new TreeSet<>();
    final Set<String> upstreams = new TreeSet<>();

    RealmBuilder(String name) {
      this.name = Objects.requireNonNull(name, "name");
    }

    RealmBuilder flag(Project.Realm.Flag flag) {
      flags.add(flag);
      return this;
    }

    RealmBuilder upstream(String upstream) {
      upstreams.add(upstream);
      return this;
    }

    RealmBuilder takeMatchingModuleInfoFilesFrom(List<Path> files) {
      if (files.isEmpty()) return this;
      if (name.isEmpty()) {
        moduleInfoFiles.addAll(files);
        files.clear();
        return this;
      }
      var iterator = files.listIterator();
      while (iterator.hasNext()) {
        var file = iterator.next();
        if (Collections.frequency(Paths.deque(file), name) == 1) {
          moduleInfoFiles.add(file);
          iterator.remove();
        }
      }
      return this;
    }

    Project.Realm build() {
      var units = new TreeMap<String, Project.Unit>();
      for (var info : moduleInfoFiles) {
        var unit = unit(info);
        units.put(unit.toName(), unit);
      }
      return new Project.Realm(name, flags, units, upstreams);
    }

    Project.Unit unit(Path info) {
      var descriptor = Modules.describe(info);
      var parent = info.getParent();
      if (parent == null) return new Project.Unit(descriptor, sources(Path.of(".")), List.of());
      var resources = parent.resolveSibling("resources");
      return new Project.Unit(
          descriptor,
          sources(parent),
          Files.isDirectory(resources) ? List.of(resources) : List.of());
    }

    List<Project.Source> sources(Path infoDirectory) {
      if (Paths.isMultiReleaseDirectory(infoDirectory)) {
        var map = new TreeMap<Integer, Path>(); // sorted by release number
        var paths = Paths.list(infoDirectory.getParent(), Files::isDirectory);
        for (var path : paths) Paths.findMultiReleaseNumber(path).ifPresent(n -> map.put(n, path));
        var sources = new ArrayList<Project.Source>();
        map.forEach((release, path) -> sources.add(new Project.Source(path, release)));
        return List.copyOf(sources);
      }
      var info = new Project.Source(infoDirectory, 0); // contains module-info.java file
      var java = infoDirectory.resolveSibling("java");
      if (java.equals(infoDirectory) || Files.notExists(java)) return List.of(info);
      return List.of(new Project.Source(java, 0), info);
    }
  }
}
