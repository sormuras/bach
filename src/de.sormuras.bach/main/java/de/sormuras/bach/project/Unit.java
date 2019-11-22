/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
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

import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public /*record*/ class Unit {
  private final Realm realm;
  private final Path info;
  private final ModuleDescriptor descriptor;
  private final List<Source> sources;
  private final List<Path> patches;

  public Unit(Realm realm, Path info, ModuleDescriptor descriptor, List<Source> sources, List<Path> patches) {
    this.realm = realm;
    this.info = info;
    this.descriptor = descriptor;
    this.sources = List.copyOf(sources);
    this.patches = List.copyOf(patches);
  }

  public ModuleDescriptor descriptor() {
    return descriptor;
  }

  public Path info() {
    return info;
  }

  public String name() {
    return descriptor.name();
  }

  public Realm realm() {
    return realm;
  }

  public List<Path> patches() {
    return patches;
  }

  @Override
  public String toString() {
    return descriptor.toNameAndVersion();
  }

  public List<Source> sources() {
    return sources;
  }

  public <T> List<T> sources(Function<Source, T> mapper) {
    return sources.stream().map(mapper).collect(Collectors.toList());
  }

  public boolean isMultiRelease() {
    return !sources.isEmpty() && sources.stream().allMatch(Source::isTargeted);
  }

  public List<Path> resources() {
    var resources = new ArrayList<Path>();
    for (var source : realm.sourcePaths()) {
      var directory = source.getParent().resolve("resources");
      var path = Path.of(directory.toString().replace("{MODULE}", name()));
      resources.add(path);
    }
    return List.copyOf(resources);
  }

  public Optional<Path> mavenPom() {
    for (var source : realm.sourcePaths()) {
      var pom = source.getParent().resolve("maven").resolve("pom.xml");
      var path = Path.of(pom.toString().replace("{MODULE}", name()));
      if (Files.isRegularFile(path)) return Optional.of(path);
    }
    return Optional.empty();
  }
}
