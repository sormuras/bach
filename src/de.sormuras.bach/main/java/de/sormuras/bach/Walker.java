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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** A builder for building {@link Project.Builder} objects by parsing a directory for modules. */
public /*static*/ class Walker {

  private Project.Base base = Project.Base.of();
  private List<Path> moduleInfoFiles = new ArrayList<>();
  private int walkDepthLimit = 9;

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

  public Walker setWalkDepthLimit(int walkDepthLimit) {
    this.walkDepthLimit = walkDepthLimit;
    return this;
  }

  public Project.Builder newBuilder() {
    var builder = Project.builder();
    builder.setBase(base);
    var directoryName = base.directory().toAbsolutePath().getFileName();
    if (directoryName != null) builder.title(directoryName.toString());
    builder.setRealms(computeRealms());
    return builder;
  }

  List<Project.Realm> computeRealms() {
    if (moduleInfoFiles.isEmpty()) walkDirectoryTreePopulatingModuleInfoFiles();
    try {
      return computeMainTestPreviewRealms();
    } catch (UnsupportedOperationException ignored) {
    }
    return computeUnnamedRealm();
  }

  List<Project.Realm> computeUnnamedRealm() {
    var unnamedRealm = new RealmBuilder("");
    unnamedRealm.moduleInfoFiles.addAll(moduleInfoFiles);
    unnamedRealm.flags.add(Project.Realm.Flag.CREATE_API_DOCUMENTATION);
    unnamedRealm.flags.add(Project.Realm.Flag.CREATE_CUSTOM_RUNTIME_IMAGE);
    unnamedRealm.flags.add(Project.Realm.Flag.LAUNCH_TESTS);
    return List.of(unnamedRealm.build());
  }

  List<Project.Realm> computeMainTestPreviewRealms() {
    var mainRealm = new RealmBuilder("main");
    // TODO mainRealm.moduleInfoFiles.add()
    mainRealm.flags.add(Project.Realm.Flag.CREATE_API_DOCUMENTATION);
    mainRealm.flags.add(Project.Realm.Flag.CREATE_CUSTOM_RUNTIME_IMAGE);
    var testRealm = new RealmBuilder("test");
    // TODO testRealm.moduleInfoFiles.add()
    testRealm.flags.add(Project.Realm.Flag.LAUNCH_TESTS);
    var previewRealm = new RealmBuilder("test-preview");
    // TODO previewRealm.moduleInfoFiles.add()
    previewRealm.flags.add(Project.Realm.Flag.LAUNCH_TESTS);
    previewRealm.flags.add(Project.Realm.Flag.ENABLE_PREVIEW_LANGUAGE_FEATURES);
    throw new UnsupportedOperationException("Not implemented, yet");
  }

  void walkDirectoryTreePopulatingModuleInfoFiles() {
    var directory = base.directory();
    if (Paths.isRoot(directory)) throw new IllegalStateException("Root directory: " + directory);
    var paths = Paths.find(List.of(directory), walkDepthLimit, Paths::isModuleInfoJavaFile);
    moduleInfoFiles.addAll(paths);
  }

  /** A builder for building {@link Project.Realm} objects. */
  private static class RealmBuilder {

    final List<Path> moduleInfoFiles = new ArrayList<>();

    final String name;
    final Set<Project.Realm.Flag> flags = new TreeSet<>();
    final Map<String, Project.Unit> units = new TreeMap<>();
    final Map<String, Project.Realm> upstreams = new TreeMap<>();

    RealmBuilder(String name) {
      this.name = Objects.requireNonNull(name, "name");
    }

    Project.Realm build() {
      for (var info : moduleInfoFiles) {
        var descriptor = Modules.describe(info);
        var module = descriptor.name();
        units.put(module, new Project.Unit(descriptor));
      }
      return new Project.Realm(name, flags, units, upstreams);
    }
  }
}
