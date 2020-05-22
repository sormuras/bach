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
import java.util.regex.Pattern;

/** A builder for building {@link Project.Builder} objects by parsing a directory for modules. */
public /*static*/ class Walker {

  /** A layout defines a directory pattern for organizing {@code module-info.java} files. */
  public enum Layout {
    /** Detect layout on-the-fly. */
    AUTOMATIC,
    /** Single unnamed realm. */
    DEFAULT,
    /** Three realms: {@code main}, {@code test}, and {@code test-preview}. */
    MAIN_TEST_PREVIEW
  }

  public static final Pattern JAVA_N_PATTERN = Pattern.compile("java.?(\\d+)");

  private Project.Base base = Project.Base.of();
  private List<Path> moduleInfoFiles = new ArrayList<>();
  private int walkDepthLimit = 9;
  private Path walkOffset = Path.of("");
  private Layout layout = Layout.AUTOMATIC;

  public Walker setBase(String directory, String... more) {
    return setBase(Path.of(directory, more));
  }

  public Walker setBase(Path directory) {
    return setBase(Project.Base.of(directory));
  }

  public Walker setBase(Project.Base base) {
    this.base = base;
    return this;
  }

  public Walker setModuleInfoFiles(List<Path> moduleInfoFiles) {
    this.moduleInfoFiles = moduleInfoFiles;
    return this;
  }

  public Walker setWalkOffset(String offset, String... more) {
    return setWalkOffset(Path.of(offset, more));
  }

  public Walker setWalkOffset(Path walkOffset) {
    this.walkOffset = walkOffset;
    return this;
  }

  public Walker setWalkDepthLimit(int walkDepthLimit) {
    this.walkDepthLimit = walkDepthLimit;
    return this;
  }

  public Walker setLayout(Layout layout) {
    this.layout = layout;
    return this;
  }

  public Project.Builder newBuilder() {
    if (moduleInfoFiles.isEmpty()) {
      var directory = base.directory().resolve(walkOffset);
      if (Paths.isRoot(directory)) throw new IllegalStateException("Root directory: " + directory);
      var paths = Paths.find(List.of(directory), walkDepthLimit, Paths::isModuleInfoJavaFile);
      if (paths.isEmpty()) throw new IllegalStateException("No module-info.java: " + directory);
      setModuleInfoFiles(paths);
    }
    var directoryName = base.directory().toAbsolutePath().getFileName();
    var builder = Project.builder();
    builder.setBase(base);
    if (directoryName != null) builder.title(directoryName.toString());
    builder.setRealms(computeRealms());
    return builder;
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

  static boolean isMultiRelease(Path directory) {
    var name = directory.getFileName().toString();
    if (name.equals("java")) return false;
    return name.startsWith("java") && JAVA_N_PATTERN.matcher(name).matches();
  }

  static boolean isMultiRelease(List<Path> directories) {
    return findMultiReleaseDirectories(directories).size() >= 1;
  }

  static List<Path> findMultiReleaseDirectories(List<Path> directories) {
    var matches = new ArrayList<Path>();
    for (var directory : directories) {
      if (isMultiRelease(directory)) matches.add(directory);
    }
    return List.copyOf(matches);
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
      var parent = info.getParent();
      var resources = parent.resolveSibling("resources");
      return new Project.Unit(
          Modules.describe(info),
          isMultiRelease(parent) ? multiReleaseSources(info) : singleReleaseSources(info),
          Files.isDirectory(resources) ? List.of(resources) : List.of());
    }

    List<Project.Source> singleReleaseSources(Path info) {
      var parent = info.getParent();
      var module = new Project.Source(parent, 0);
      var java = parent.resolveSibling("java");
      if (parent.equals(java) || Files.notExists(java)) return List.of(module);
      return List.of(new Project.Source(java, 0), module);
    }

    List<Project.Source> multiReleaseSources(Path info) {
      var map = new TreeMap<Integer, Path>();
      var paths = Paths.list(info.getParent().getParent(), Files::isDirectory);
      for (var path : paths) {
        var matcher = JAVA_N_PATTERN.matcher(path.getFileName().toString());
        if (!matcher.matches()) continue;
        map.put(Integer.parseInt(matcher.group(1)), path);
      }
      if (map.isEmpty()) throw new IllegalStateException("No matching source found: " + paths);
      var sources = new ArrayList<Project.Source>();
      for (var entry : map.entrySet())
        sources.add(new Project.Source(entry.getValue(), entry.getKey()));
      return List.copyOf(sources);
    }
  }
}
